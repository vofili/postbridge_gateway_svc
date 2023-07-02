package com.tms.service.controller;

import com.tms.lib.apimodel.KeyDTO;
import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.terminals.services.TmsCtmkService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ctmk")
@RequiredArgsConstructor
public class TmsCtmkController {

    private final TmsCtmkService tmsCtmkService;

    @PostMapping("/create")
    public KeyDTO createTmsCtmk(@RequestBody @Validated KeyDTO keyDTO) throws ServiceProcessingException {
        return tmsCtmkService.createCtmk(keyDTO);
    }
}
