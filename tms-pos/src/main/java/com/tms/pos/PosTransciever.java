package com.tms.pos;

import com.tms.lib.exceptions.InterchangeServiceException;
import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.helper.ISOSourceNodeHelper;
import com.tms.lib.network.transciever.TranscieveFunction;
import com.tms.lib.processor.SourceTransactionProcessor;
import com.tms.lib.router.Router;
import com.tms.lib.transactionrecord.service.TransactionRecordService;
import com.tms.lib.util.ByteUtils;
import com.tms.pos.service.TerminalMacValidator;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.springframework.context.ApplicationContext;

import java.util.List;

@Slf4j
public class PosTransciever implements TranscieveFunction<ISOMsg, byte[]> {

    private ISOSourceNodeHelper sourceNodeHelper;
    private ISOPackager packager = new PosPackager();
    private TerminalMacValidator terminalMacValidator;


    PosTransciever(List<SourceTransactionProcessor> processors, TransactionRecordService transactionRecordService,
                   PosInterchange posInterchange, Router router, ApplicationContext context) {

        sourceNodeHelper = new ISOSourceNodeHelper(processors, transactionRecordService, posInterchange, router);
        this.terminalMacValidator = context.getBean(TerminalMacValidator.class);
    }

    @Override
    public byte[] transcieve(ISOMsg receivedIsoMsg) throws InterchangeServiceException {
        try {
            if (!terminalMacValidator.isValidMessage(receivedIsoMsg)) {
                throw new InterchangeServiceException("Invalid message received, could not validate hash value sent");
            }
            ISOMsg response = sourceNodeHelper.processMessage(receivedIsoMsg);
            response.setPackager(packager);
            return ByteUtils.prependLenBytes(response.pack());
        } catch (ISOException e) {
            throw new InterchangeServiceException("Could not unpack message", e);
        } catch (TransactionProcessingException e) {
            throw new InterchangeServiceException("Could not process iso message", e);
        } catch (ServiceProcessingException e) {
            throw new InterchangeServiceException("Could not validate pos message", e);
        }
    }
}
