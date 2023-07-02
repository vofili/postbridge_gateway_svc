package com.tms.service.controller;

import com.tms.lib.apimodel.KeyDTO;
import com.tms.lib.exceptions.InterchangeServiceException;
import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.interchange.InterchangeConfigService;
import com.tms.lib.interchange.InterchangeFactory;
import com.tms.service.apimodel.InterchangeConfigDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/interchange")
@RequiredArgsConstructor
@Slf4j
public class InterchangeController {

    private final InterchangeConfigService interchangeConfigService;
    private final InterchangeFactory interchangeFactory;

    @PostMapping("/create")
    public InterchangeConfigDTO createInterchange(@RequestBody @Valid InterchangeConfigDTO interchangeConfigDTO) {
        interchangeConfigService.saveInterchange(interchangeConfigDTO.toInterchangeConfig());
        interchangeFactory.reloadInterchanges();
        return interchangeConfigDTO;
    }

    @PostMapping("/add-key/{code}")
    public KeyDTO addInterchangeKey(@PathVariable("code") String code, @RequestBody @Validated KeyDTO keyDTO) throws ServiceProcessingException {
        return interchangeConfigService.updateInterchangeKey(code, keyDTO);
    }

    @PostMapping(value = "/activate/{interchangeId}/{active}")
    public boolean setInterchangeActive(@PathVariable Integer interchangeId, @PathVariable boolean active) {
        try {
            if (active) {
                return interchangeFactory.startInterchange(interchangeId);
            } else {
                return interchangeFactory.stopInterchange(interchangeId);
            }
        } catch (InterchangeServiceException e) {
            log.error("There was an error setting interchange state", e);
            return false;
        }
    }

    @PostMapping("/reload")
    public void reloadInterchanges() {
        interchangeFactory.reloadInterchanges();
    }
}
