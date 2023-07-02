package com.tms.lib.hsm;

import com.tms.lib.exceptions.HsmException;
import com.tms.lib.hsm.model.GeneratedKeyMessage;
import com.tms.lib.hsm.model.PinTranslationRequest;

public interface HsmService {

    GeneratedKeyMessage generateTMKAndEncryptUnderZmk(String encryptionKey) throws HsmException;

    GeneratedKeyMessage generateTSKAndEncryptUnderTmk(String encryptionKey) throws HsmException;

    GeneratedKeyMessage generateTPKAndEncryptUnderTmk(String encryptionKey) throws HsmException;

    GeneratedKeyMessage convertZpkUnderZmkToZpkUnderLmk(String zpkUnderZmk, String zmkUnderLmk) throws HsmException;

    String translatePinBlockFromTpkToDestinationZpk(PinTranslationRequest pinTranslationRequest) throws HsmException;
}
