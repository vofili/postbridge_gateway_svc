package com.tms.pos.service.impl;

import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.exceptions.ServiceRuntimeException;
import com.tms.lib.interchange.InterchangeConfig;
import com.tms.lib.interchange.InterchangeConfigService;
import com.tms.lib.network.TmsInterchangeClientManager;
import com.tms.lib.provider.DefaultStanAndRrnProvider;
import com.tms.lib.security.Encrypter;
import com.tms.lib.service.TerminalKeyDownloadService;
import com.tms.lib.util.IsoLogger;
import com.tms.lib.util.IsoUtil;
import com.tms.pos.PosPackager;
import com.tms.pos.entities.MappedNibssTerminal;
import com.tms.pos.poskeyspersisters.impl.MasterKeyPersister;
import com.tms.pos.poskeyspersisters.impl.PinKeyPersister;
import com.tms.pos.poskeyspersisters.impl.SessionKeyPersister;
import com.tms.pos.service.NibssTerminalIdMappingService;
import com.tms.pos.service.TerminalMacValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static com.tms.pos.utils.POSMessageUtils.TERMINAL_ID_FIELD;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "destination.terminal.key.download.enabled", havingValue = "true")
public class TerminalKeyDownloadServiceImpl implements TerminalKeyDownloadService {

    private final NibssTerminalIdMappingService destinationTerminalIdMappingService;
    private final InterchangeConfigService interchangeConfigService;
    private final MasterKeyPersister masterKeyPersister;
    private final SessionKeyPersister sessionKeyPersister;
    private final PinKeyPersister pinKeyPersister;
    private final TerminalMacValidator terminalMacValidator;
    private final DefaultStanAndRrnProvider defaultStanAndRrnProvider;
    private final Encrypter encrypter;

    private PosPackager packager = new PosPackager();

    @Override
    public void downloadKeysForMappedTerminal(String terminalId) {

        List<MappedNibssTerminal> mappedDestinationTerminals = destinationTerminalIdMappingService.getAllPairOfMappedTerminalId(terminalId);

        for (MappedNibssTerminal mappedDestinationTerminal : mappedDestinationTerminals) {

            try {
                int configId = mappedDestinationTerminal.getInterchangeConfigId();
                InterchangeConfig interchangeConfig = interchangeConfigService.findById(configId)
                        .orElseThrow(() -> new ServiceProcessingException(String.format("Cannot find mapped interchange config with id %d" +
                                ". Aborting key download...", configId)));

                if (!interchangeConfig.isActive()) {
                    log.info("Interchange {} is not active, no key download done for mapped terminal {}", interchangeConfig.getName(), mappedDestinationTerminal.getNibssTerminalId());
                    continue;
                }

                String encryptedInterchangeKey = interchangeConfig.getEncryptedInterchangeKey();

                String ctmk = encrypter.decrypt(encryptedInterchangeKey);

                ISOMsg isoMsg = new ISOMsg();
                Date now = new Date();
                isoMsg.set(7, IsoUtil.transmissionDateAndTime(now));
                isoMsg.set(11, defaultStanAndRrnProvider.getNextStan());
                isoMsg.set(12, IsoUtil.timeLocalTransaction(now));
                isoMsg.set(13, IsoUtil.dateLocalTransaction(now));
                isoMsg.set(41, terminalId);


                doMasterKeyDownload(isoMsg, ctmk, mappedDestinationTerminal, interchangeConfig);
                doSessionKeyDownload(isoMsg, mappedDestinationTerminal, interchangeConfig);
                doPinKeyDownload(isoMsg, mappedDestinationTerminal, interchangeConfig);
                doParametersDownload(isoMsg, mappedDestinationTerminal, interchangeConfig);
            } catch (Exception e) {
                log.error("Destination Key download failed for terminal {} and destination tid {}", terminalId, mappedDestinationTerminal.getNibssTerminalId(), e);
            }
        }
    }

    private void doMasterKeyDownload(ISOMsg isoMsg, String ctmk, MappedNibssTerminal mappedDestinationTerminal, InterchangeConfig interchangeConfig) {
        try {
            isoMsg.set(3, "9A0000");
            ISOMsg destinationISOMsg = prepareISOMsgForDestination(isoMsg, mappedDestinationTerminal);

            ISOMsg responseISO = processMessage(interchangeConfig, destinationISOMsg);
            masterKeyPersister.persistKeyFromResponse(responseISO, ctmk);
        } catch (ServiceProcessingException e) {
            throw new ServiceRuntimeException("Could not successfully do master key download", e);
        }
    }

    private void doSessionKeyDownload(ISOMsg isoMsg, MappedNibssTerminal mappedDestinationTerminal, InterchangeConfig interchangeConfig) {
        try {
            isoMsg.set(3, "9B0000");
            sessionKeyPersister.persistKeyFromResponse(doISOExchange(isoMsg, interchangeConfig, mappedDestinationTerminal));
        } catch (ServiceProcessingException e) {
            throw new ServiceRuntimeException("Could not successfully do session key download", e);
        }
    }

    private void doPinKeyDownload(ISOMsg isoMsg, MappedNibssTerminal mappedDestinationTerminal, InterchangeConfig interchangeConfig) {
        try {
            isoMsg.set(3, "9G0000");
            pinKeyPersister.persistKeyFromResponse(doISOExchange(isoMsg, interchangeConfig, mappedDestinationTerminal));
        } catch (ServiceProcessingException e) {
            throw new ServiceRuntimeException("Could not successfully do pin key download", e);
        }
    }

    public void doParametersDownload(ISOMsg isoMsg, MappedNibssTerminal mappedDestinationTerminal, InterchangeConfig interchangeConfig) {
        try {
            isoMsg.set(3, "9C0000");
            isoMsg.set(62, "01008" + isoMsg.getString(TERMINAL_ID_FIELD));
            isoMsg.set(64, new String(new byte[]{0x0}));
            ISOMsg destinationISOMsg = prepareISOMsgForDestination(isoMsg, mappedDestinationTerminal);
            String macField = terminalMacValidator.generateHashForIsoMsg(destinationISOMsg);
            destinationISOMsg.set(64, macField);
            ISOMsg responseISO = processMessage(interchangeConfig, destinationISOMsg);
            log.info("Params download response from destination {}", IsoLogger.dump(responseISO));
        } catch (ServiceProcessingException e) {
            throw new ServiceRuntimeException("Could not successfully do parameters download", e);
        }
    }

    private ISOMsg doISOExchange(ISOMsg isoMsg, InterchangeConfig interchangeConfig, MappedNibssTerminal mappedDestinationTerminal) throws ServiceProcessingException {
        ISOMsg destinationISOMsg = prepareISOMsgForDestination(isoMsg, mappedDestinationTerminal);

        return processMessage(interchangeConfig, destinationISOMsg);
    }

    private ISOMsg prepareISOMsgForDestination(ISOMsg isoMsg, MappedNibssTerminal mappedDestinationTerminal) throws ServiceProcessingException {
        if (isoMsg == null) {
            throw new ServiceProcessingException("Iso Msg supplied is null. Aborting master key download...");
        }

        String destinationTerminalId = mappedDestinationTerminal.getNibssTerminalId();

        ISOMsg destinationISOMsg = new ISOMsg();
        try {
            destinationISOMsg.setMTI("0800");
        } catch (ISOException e) {
            throw new ServiceProcessingException("Could not set mti for destination iso msg", e);
        }
        IsoUtil.copyFields(isoMsg, destinationISOMsg);

        destinationISOMsg.set(TERMINAL_ID_FIELD, destinationTerminalId);

        destinationISOMsg.setPackager(packager);
        return destinationISOMsg;
    }

    private ISOMsg processMessage(InterchangeConfig interchangeConfig, ISOMsg destinationISOMsg) throws ServiceProcessingException {

        try {
            return TmsInterchangeClientManager.send(destinationISOMsg, interchangeConfig, packager);
        } catch (Exception e) {
            throw new ServiceProcessingException(
                    String.format("Could not successfully do key download for mapped terminal id %s", destinationISOMsg.getString(TERMINAL_ID_FIELD)), e);
        }
    }


}
