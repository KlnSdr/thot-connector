package thot.connector;

import common.inject.api.Inject;
import common.inject.api.RegisterFor;
import common.logger.Logger;
import dobby.IConfig;
import thot.api.command.Command;
import thot.api.command.CommandType;
import thot.api.command.KeyType;
import thot.api.payload.CreatePayload;
import thot.api.payload.DeletePayload;
import thot.api.payload.ReadPayload;
import thot.api.payload.WritePayload;
import thot.api.response.Response;
import thot.api.response.ResponseType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.Socket;

@RegisterFor(IConnector.class)
public class Connector implements IConnector {
    private static final Logger LOGGER = new Logger(Connector.class);
    private final IConfig config;

    @Inject
    public Connector(IConfig config) {
        this.config = config;
    }

    private Response sendRequest(Command command) throws IOException, ClassNotFoundException {
        final String dbHost = config.getBoolean("application.devMode", true)
                ? "localhost"
                : config.getString("application.dbHost", "localhost");
        final Socket socket = new Socket(dbHost, 12903);

        final ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());

        ostream.writeObject(command);
        ostream.flush();

        final ObjectInputStream istream = new ObjectInputStream(socket.getInputStream());

        final Response response = (Response) istream.readObject();

        ostream.close();
        istream.close();
        socket.close();

        return response;
    }

    public boolean create(String bucketName) {
        return create(bucketName, 100);
    }

    public boolean create(String bucketName, int maxKeys) {
        return create(bucketName, maxKeys, false);
    }

    public boolean create(String bucketName, int maxKeys, boolean isVolatile) {
        try {
            final Command command = new Command(CommandType.CREATE, bucketName, new CreatePayload(bucketName, maxKeys, isVolatile));
            final Response response = sendRequest(command);

            if (response.getResponseType() == ResponseType.ERROR) {
                LOGGER.error("Failed to create bucket " + bucketName);
                LOGGER.error(response.getError());
                return false;
            }
            return true;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace(e);
            return false;
        }
    }

    public boolean writeCreateVolatile(String bucketName, String key, Serializable value) {
        return write(bucketName, key, value, true);
    }

    public boolean write(String bucketName, String key, Serializable value) {
        return write(bucketName, key, value, false);
    }

    private boolean write(String bucketName, String key, Serializable value, boolean createVolatileBucket) {
        try {
            Command command = new Command(CommandType.WRITE, bucketName, new WritePayload(key, value, createVolatileBucket));
            Response response = sendRequest(command);

            return response.getResponseType() == ResponseType.SUCCESS;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace(e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T[] readPattern(String bucketName, String pattern, Class<? extends T> comonentTypeClass) {
        try {
            Command command = new Command(CommandType.READ, bucketName, new ReadPayload(pattern, KeyType.REGEX));
            Response response = sendRequest(command);
            Object[] responseArray = (Object[]) response.getValue();
            if (responseArray.length == 0) {
                return (T[]) Array.newInstance(comonentTypeClass, 0);
            } else if (comonentTypeClass.isInstance(responseArray[0])) {
                T[] arr = (T[]) Array.newInstance(comonentTypeClass, responseArray.length);
                for (int i = 0; i < responseArray.length; i++) {
                    arr[i] = comonentTypeClass.cast(responseArray[i]);
                }
                return arr;
            } else {
                return null;
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace(e);
            return null;
        }
    }

    public <T> T read(String bucketName, String key, Class<? extends T> typeClass) {
        try {
            Command command = new Command(CommandType.READ, bucketName, new ReadPayload(key, KeyType.ABSOLUTE));
            Response response = sendRequest(command);
            if (typeClass.isInstance(response.getValue())) {
                return typeClass.cast(response.getValue());
            } else {
                return null;
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace(e);
            return null;
        }
    }

    public String[] getKeys(String bucketName) {
        try {
            Command command = new Command(CommandType.KEYS, bucketName, null);
            Response response = sendRequest(command);
            if (response.getResponseType() == ResponseType.SUCCESS) {
                return (String[]) response.getValue();
            } else {
                return new String[0];
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace(e);
            return new String[0];
        }
    }

    public String[] getBuckets() {
        try {
            Command command = new Command(CommandType.BUCKETS, null, null);
            Response response = sendRequest(command);
            if (response.getResponseType() == ResponseType.SUCCESS) {
                return (String[]) response.getValue();
            } else {
                return new String[0];
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace(e);
            return new String[0];
        }
    }

    public boolean deletePattern(String bucketName, String pattern) {
        return delete(bucketName, pattern, KeyType.REGEX);
    }

    public boolean delete(String bucketName, String key) {
        return delete(bucketName, key, KeyType.ABSOLUTE);
    }

    private boolean delete(String bucketName, String key, KeyType keyType) {
        try {
            Command command = new Command(CommandType.DELETE, bucketName, new DeletePayload(key, keyType));
            Response response = sendRequest(command);

            return response.getResponseType() == ResponseType.SUCCESS;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.trace(e);
            return false;
        }
    }
}
