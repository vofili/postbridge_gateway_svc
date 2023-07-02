package com.tms.lib.interchange;

import com.tms.lib.exceptions.InterchangeConstructionException;
import com.tms.lib.exceptions.InterchangeServiceException;
import com.tms.lib.transactionrecord.service.TransactionRecordService;
import com.tms.lib.util.AsyncWorkerPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterchangeFactory {

    private final Map<Integer, InterchangeConfig> configs = new ConcurrentHashMap<>();
    private final Map<Integer, Interchange> interchanges = new ConcurrentHashMap<>();
    private final Map<String, Interchange> interchangeFactories = new ConcurrentHashMap<>();

    private final InterchangeConfigService interchangeConfigService;
    private final ApplicationContext context;
    private final TransactionRecordService transactionRecordService;
    private final AsyncWorkerPool workerPool;

    @PostConstruct
    public void init() {
        loadAllInterchangeFactories();
        reloadInterchanges();
    }

    private void loadAllInterchangeFactories() {
        interchangeFactories.clear();
        List<Interchange> prototypes = getPrototypesForImplementing(Interchange.class,
                "com.tms");
        for (Interchange prototype : prototypes) {
            interchangeFactories.put(prototype.getTypeName(), prototype);
        }
    }

    public static <T> List<T> getPrototypesForImplementing(Class<T> prototypeInterface, String namespaceFilter) {
        List<T> objects = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.resetFilters(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(prototypeInterface));
        for (BeanDefinition bd : scanner.findCandidateComponents(namespaceFilter)) {
            try {
                Class clazz = (Class.forName(bd.getBeanClassName()));
                T object = (T) clazz.newInstance();
                objects.add(object);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                log.error("There was an error initializing prototypes", ex);
            }
        }
        return objects;
    }

    public boolean startInterchange(int configId) throws InterchangeServiceException {
        InterchangeConfig interchangeConfig = configs.get(configId);
        if (interchangeConfig == null) {
            return false;
        }
        interchangeConfig.setActive(true);
        Interchange interchange = interchanges.get(configId);
        if (interchange == null) {
            return false;
        }
        if (!interchange.isStarted()) {
            interchange.start();
        }
        interchangeConfigService.saveInterchange(interchangeConfig);
        return true;
    }

    public boolean stopInterchange(int configId) throws InterchangeServiceException {
        InterchangeConfig interchangeConfig = configs.get(configId);
        if (interchangeConfig == null) {
            throw new InterchangeServiceException("Interchange config is null cannot stop a null interchange");
        }
        Interchange interchange = interchanges.get(configId);
        if (interchange == null) {
            throw new InterchangeServiceException("Cannot stop a null interchange");
        }
        log.info(String.format("Stopping interchange %s", interchangeConfig.getName()));
        interchange.stop();
        interchangeConfig.setActive(false);
        interchangeConfigService.saveInterchange(interchangeConfig);
        return true;
    }

    private boolean reloadInterchangeIfNeeded(InterchangeConfig config) {
        Interchange interchange = interchanges.get(config.getId());
        if (interchange == null) {
            return false;
        }
        try {
            if (interchange.setConfig(config)) {
                interchange.stop();
                interchange = getInterchangeForConfig(config);
                if (interchange == null) {
                    return false;
                }
                if (config.isActive()) {
                    interchange.start();
                }
                interchanges.put(config.getId(), interchange);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("There was an error reloading old interchange", e);
            return false;
        }
    }

    private boolean loadNewInterchange(InterchangeConfig config) {
        try {
            Interchange interchange = getInterchangeForConfig(config);
            if (interchange == null) {
                throw new InterchangeServiceException(String.format("Interchange %s could not be created because prototype was not found", config.getName()));
            }
            interchanges.put(config.getId(), interchange);
            if (config.isActive()) {
                try {
                    interchange.start();
                    return true;
                } catch (InterchangeServiceException e) {
                    log.error("There was an error starting interchange", e);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Error loading new interchange", e);
            return false;
        }
    }

    public void reloadInterchanges() {
        List<Integer> newIds = new ArrayList<>();

        List<InterchangeConfig> newList = interchangeConfigService.getAllInterchangeConfigs();
        for (InterchangeConfig config : newList) {
            newIds.add(config.getId());
            /**
             * either this config is new or it is existing but updated
             */
            boolean isNew = !configs.containsKey(config.getId());
            configs.put(config.getId(), config);
            if (isNew) {
                loadNewInterchange(config);

            } else {
                reloadInterchangeIfNeeded(config);
            }
        }
        /**
         * Get a list of all configs in the old list and not in new, stop
         * them all and remove reference
         */
        List<Integer> ids = configs.keySet()
                .stream()
                .filter(c -> !newIds.contains(c))
                .collect(Collectors.toList());
        for (Integer id : ids) {
            configs.remove(id);
            Interchange interchange = interchanges.get(id);
            if (interchange == null) {
                continue; //wft?
            }
            try {
                if (interchange.isStarted()) {
                    interchange.stop();
                }
            } catch (InterchangeServiceException e) {
                log.error("There was an error reloading interchanges", e);
            }
            interchanges.remove(id);
        }
    }

    private Interchange getInterchangeForConfig(InterchangeConfig interchangeConfig) throws InterchangeConstructionException {
        Interchange prototype = interchangeFactories.get(interchangeConfig.getTypeName());
        if (prototype == null) {
            return null;
        }
        return prototype.construct(interchangeConfig, workerPool, context, transactionRecordService);
    }

    public List<Interchange> getAllInterchanges() {
        List<Interchange> results = new ArrayList<>();
        interchanges.forEach((k, v) -> results.add(v));
        return results;
    }

    public List<Interchange> getAllInterchangeProtoptypes() {
        List<Interchange> results = new ArrayList<>();
        List<String> singletonInterchangeTypeNames = getAllSingletonInterchanges();
        results.addAll(interchangeFactories.entrySet().stream().filter(entry -> !entry.getValue().isSingleton() || !singletonInterchangeTypeNames.contains(entry.getValue().getTypeName())).map(Map.Entry<String, Interchange>::getValue).collect(Collectors.toList()));
        return results;
    }

    private List<String> getAllSingletonInterchanges() {
        return interchanges.entrySet().stream().filter(entry -> entry.getValue().isSingleton()).map(entry -> entry.getValue().getTypeName()).collect(Collectors.toList());
    }

    public Interchange getInterchange(int interchangeConfigId) {
        return interchanges.get(interchangeConfigId);
    }

    public Interchange getFirstMatchingInterchangeOfType(String typeName) {
        synchronized (interchanges) {
            for (Interchange interchange : interchanges.values()) {
                if (interchange.getTypeName().equals(typeName)) {
                    return interchange;
                }
            }
        }
        return null;
    }

    public Interchange getInterchange(String name) {
        synchronized (interchanges) {
            for (Interchange interchange : interchanges.values()) {
                if (interchange.getName().equals(name)) {
                    return interchange;
                }
            }
        }
        return null;
    }

    public Boolean deleteInterchange(int interchangeConfigId) throws InterchangeServiceException {
        if (stopInterchange(interchangeConfigId)) {
            interchangeConfigService.deleteInterchange(interchangeConfigId);
            reloadInterchanges();
            return true;
        }
        return false;
    }

    public List<Interchange> getSinkNodeInterchanges() {
        List<Interchange> sinkNodeInterchanges = new ArrayList<>();
        synchronized (interchanges) {
            for (Interchange interchange : interchanges.values()) {
                for (InterchangeMode mode : interchange.getSupportedModes()) {
                    if (mode.equals(InterchangeMode.SinkMode)) {
                        sinkNodeInterchanges.add(interchange);
                        break;
                    }
                }
            }
        }

        return sinkNodeInterchanges;
    }

    public boolean isInterchangeRunning(int configId) {
        Interchange interchange = interchanges.get(configId);
        if (interchange == null) {
            return false;
        }
        return interchange.isStarted();
    }

    public String getInterchangeName(Long sourceId) {
        InterchangeConfig interchangeConfig = configs.get(sourceId.intValue());
        if (interchangeConfig == null) {
            return null;
        }
        return interchangeConfig.getName();
    }
}
