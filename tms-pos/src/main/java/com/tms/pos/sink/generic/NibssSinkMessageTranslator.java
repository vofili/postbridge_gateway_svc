package com.tms.pos.sink.generic;

import com.tms.lib.exceptions.HsmException;
import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.hsm.HsmService;
import com.tms.lib.hsm.model.PinTranslationRequest;
import com.tms.lib.terminals.services.TerminalKeyService;
import com.tms.lib.util.IsoUtil;
import com.tms.pos.PosPackager;
import com.tms.pos.service.NibssTerminalIdMappingService;
import com.tms.pos.service.TerminalMacValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;

import static com.tms.pos.utils.POSMessageUtils.CARD_ACCEPTOR_ID_FIELD;
import static com.tms.pos.utils.POSMessageUtils.TERMINAL_ID_FIELD;

@Service
@RequiredArgsConstructor
@Slf4j
public class NibssSinkMessageTranslator {

    private final NibssTerminalIdMappingService terminalIdMappingService;
    private final TerminalMacValidator terminalMacValidator;
    private final HsmService hsmService;
    private final TerminalKeyService terminalKeyService;

    public ISOMsg translateISOMsgToNibssEquivalent(ISOMsg isoMsg, int interchangeConfigId) throws ServiceProcessingException {
        if (isoMsg == null) {
            throw new ServiceProcessingException("Iso Msg supplied is null. Aborting master key download...");
        }

        ISOMsg nibssIsoMsg = new ISOMsg();

        try {
            nibssIsoMsg.setMTI("0200");
        } catch (ISOException e) {
            throw new ServiceProcessingException("Could not set mti for nibss iso msg", e);
        }

        IsoUtil.copyFields(isoMsg, nibssIsoMsg);

        nibssIsoMsg.setPackager(new PosPackager());

        enrichWithNibssTerminalIdAndCardAcceptorId(nibssIsoMsg, interchangeConfigId);

        updatePinBlockWithDestinationEncryption(nibssIsoMsg, isoMsg.getString(TERMINAL_ID_FIELD));

        updateMacFieldWithDestinationEncryption(nibssIsoMsg);

        return nibssIsoMsg;
    }

    private ISOMsg enrichWithNibssTerminalIdAndCardAcceptorId(ISOMsg nibssIsoMsg, int interchangeConfigId) {
        String tmsTerminalId = nibssIsoMsg.getString(TERMINAL_ID_FIELD);
        Pair<String, String> nibssTerminalIdToCardAcceptorPair =
                terminalIdMappingService.getNibssMappedTerminalIdAndMerchantIdPair(tmsTerminalId, interchangeConfigId);

        String nibssTerminalId = nibssTerminalIdToCardAcceptorPair.getLeft();
        String nibssCardAcceptorId = nibssTerminalIdToCardAcceptorPair.getRight();

        nibssIsoMsg.set(TERMINAL_ID_FIELD, nibssTerminalId);
        nibssIsoMsg.set(CARD_ACCEPTOR_ID_FIELD, nibssCardAcceptorId);

        return nibssIsoMsg;
    }

    private ISOMsg updatePinBlockWithDestinationEncryption(ISOMsg nibssIsoMsg, String sourceTid) throws ServiceProcessingException {
        String sourceEncryptedPinBlock = nibssIsoMsg.getString(52);

        if (StringUtils.isEmpty(sourceEncryptedPinBlock)) {
            return nibssIsoMsg;

        }
        String sourceEncryptionKey;
        try {
            sourceEncryptionKey = terminalKeyService.getConfiguredTerminalPinKey(sourceTid);
        } catch (ServiceProcessingException e) {
            throw new ServiceProcessingException("Could not get configured source terminal pin key");
        }

        String destinationEncryptionKey;
        try {
            destinationEncryptionKey = terminalKeyService.getConfiguredTerminalPinKey(nibssIsoMsg.getString(TERMINAL_ID_FIELD));
        } catch (ServiceProcessingException e) {
            throw new ServiceProcessingException("Could not get configured destination terminal pin key", e);
        }


        PinTranslationRequest pinTranslationRequest = PinTranslationRequest.builder()
                .pinBlock(sourceEncryptedPinBlock)
                .sourceZpk(sourceEncryptionKey)
                .destinationZpk(destinationEncryptionKey)
                .build();

        String destinationEncryptedPinBlock;
        try {
            destinationEncryptedPinBlock = hsmService.translatePinBlockFromTpkToDestinationZpk(pinTranslationRequest);
        } catch (HsmException e) {
            throw new ServiceProcessingException("Could not translate pin block from source zpk to destination zpk", e);
        }

        nibssIsoMsg.set(52, destinationEncryptedPinBlock.toUpperCase());

        return nibssIsoMsg;
    }

    private ISOMsg updateMacFieldWithDestinationEncryption(ISOMsg nibssIsoMsg) throws ServiceProcessingException {
        String macField = terminalMacValidator.generateHashForIsoMsg(nibssIsoMsg);
        nibssIsoMsg.set(128, macField);

        return nibssIsoMsg;
    }
}
