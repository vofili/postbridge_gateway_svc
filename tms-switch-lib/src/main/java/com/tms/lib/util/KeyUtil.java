package com.tms.lib.util;

import com.tms.lib.exceptions.UtilOperationException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class KeyUtil {

    private KeyUtil(){

    }

    public static Pair<String, String> combineKey(String component1, String component2) throws UtilOperationException {
        if (StringUtils.isEmpty(component1) || StringUtils.isEmpty(component2)){
            throw new UtilOperationException("Components must not be null or empty");
        }
        try {
            byte[] firstCompBytes = Hex.decodeHex(component1.toCharArray());
            byte[] secondCompBytes = Hex.decodeHex(component2.toCharArray());

            byte[] combinedComponentsBytes = ByteUtils.exclusiveOr(firstCompBytes, secondCompBytes);
            byte[] kcvBytes = TDesEncryptionUtil.generateKeyCheckValue(combinedComponentsBytes);

            String combinedComponents = new String(Hex.encodeHex(combinedComponentsBytes)).toUpperCase();
            String generatedKcv = new String(Hex.encodeHex(kcvBytes)).toUpperCase();

            return new ImmutablePair<>(combinedComponents, generatedKcv);

        }catch (Exception e){
            throw new UtilOperationException("Could not combine key components", e);
        }
    }

    public static void main(String[] args) throws UtilOperationException {
        System.out.println(KeyUtil.combineKey("24CCA353D2BBB778509AB13FA5BD50F9", "4662402FD6DAD65DB5E0087A3E6BEF19"));
    }
}
