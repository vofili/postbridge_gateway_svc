package com.tms.lib.security;

import com.tms.lib.exceptions.CryptoException;

public interface Encrypter {

    String encrypt(String data) throws CryptoException;

    String decrypt(String data) throws CryptoException;
}
