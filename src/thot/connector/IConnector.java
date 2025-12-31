package thot.connector;

import java.io.Serializable;

public interface IConnector {

    boolean create(String bucketName);

    boolean create(String bucketName, int maxKeys);

    boolean create(String bucketName, int maxKeys, boolean isVolatile);

    boolean writeCreateVolatile(String bucketName, String key, Serializable value);

    boolean write(String bucketName, String key, Serializable value);

    <T> T[] readPattern(String bucketName, String pattern, Class<? extends T> comonentTypeClass);

    <T> T read(String bucketName, String key, Class<? extends T> typeClass);

    String[] getKeys(String bucketName);

    String[] getBuckets();

    boolean deletePattern(String bucketName, String pattern);

    boolean delete(String bucketName, String key);
}
