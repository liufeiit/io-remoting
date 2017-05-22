package io.remoting.protocol;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月19日 下午5:40:56
 */
public enum SerializeType {

    Jackson(0, new JacksonSerializer())
    
    ;
    
    private final int serializeCode;
    
    private final Serializer serializer;

    private SerializeType(int serializeCode, Serializer serializer) {
        this.serializeCode = serializeCode;
        this.serializer = serializer;
    }

    public static SerializeType valueOf(int serializeCode) {
        for (SerializeType serializeType : values()) {
            if (serializeType.serializeCode == serializeCode) {
                return serializeType;
            }
        }
        throw new RuntimeException("don't support SerializeType code : " + serializeCode);
    }
    
    public Serializer getSerializer() {
        return serializer;
    }
    
    public int getSerializeCode() {
        return serializeCode;
    }
}
