package dev.dead;

import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.EncoderException;

public class Main {
    static void main() {
        BinaryEncoder binaryEncoder = new BinaryEncoder() {
            @Override
            public byte[] encode(byte[] source) throws EncoderException {
                return new byte[0];
            }

            @Override
            public Object encode(Object source) throws EncoderException {
                return null;
            }
        };


    }
}
