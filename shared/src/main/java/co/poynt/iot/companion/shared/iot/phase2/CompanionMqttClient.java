package co.poynt.iot.companion.shared.iot.phase2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import co.poynt.iot.companion.shared.logging.EvidenceLogger;
import software.amazon.awssdk.crt.mqtt5.Mqtt5Client;
import software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions;
import software.amazon.awssdk.crt.mqtt5.OnAttemptingConnectReturn;
import software.amazon.awssdk.crt.mqtt5.OnConnectionFailureReturn;
import software.amazon.awssdk.crt.mqtt5.OnConnectionSuccessReturn;
import software.amazon.awssdk.crt.mqtt5.OnDisconnectionReturn;
import software.amazon.awssdk.crt.mqtt5.OnStoppedReturn;
import software.amazon.awssdk.crt.mqtt5.PublishReturn;
import software.amazon.awssdk.crt.mqtt5.QOS;
import software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket;
import software.amazon.awssdk.crt.mqtt5.packets.PublishPacket;
import software.amazon.awssdk.crt.mqtt5.packets.SubAckPacket;
import software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket;
import software.amazon.awssdk.iot.AwsIotMqtt5ClientBuilder;

/**
 * Production {@code AWSIoTClient} connect path: MQTT5 + custom authorizer JWT.
 */
public final class CompanionMqttClient implements Mqtt5ClientOptions.LifecycleEvents,
        Mqtt5ClientOptions.PublishEvents {

    public interface Listener {
        void onConnect();

        void onDisconnect();

        void onError(@NonNull String error);

        void onMessage(@NonNull String topic, @NonNull String payload);
    }

    private final EvidenceLogger logger;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final Object lock = new Object();

    private volatile Mqtt5Client client;
    private volatile Listener listener;
    private volatile CompletableFuture<Boolean> connectFuture;

    public CompanionMqttClient(@NonNull EvidenceLogger logger) {
        this.logger = logger;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public boolean isConnecting() {
        return connecting.get();
    }

    @NonNull
    public CompletableFuture<Boolean> connect(@NonNull MqttConnectParams params) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        synchronized (lock) {
            if (connected.get()) {
                future.complete(true);
                return future;
            }
            if (connecting.get() && connectFuture != null) {
                return connectFuture;
            }
            connecting.set(true);
            connectFuture = future;
        }
        try {
            stopQuietly();
            AwsIotMqtt5ClientBuilder.MqttConnectCustomAuthConfig customAuth =
                    new AwsIotMqtt5ClientBuilder.MqttConnectCustomAuthConfig();
            customAuth.authorizerName = params.authorizerName;
            customAuth.tokenKeyName = ProductionIotConstants.DEFAULT_TOKEN_KEY_NAME;
            customAuth.tokenValue = params.jwt;
            customAuth.tokenSignature = "";
            customAuth.username = "";

            ConnectPacket.ConnectPacketBuilder connectPacket = new ConnectPacket.ConnectPacketBuilder();
            connectPacket.withClientId(params.clientId);
            connectPacket.withSessionExpiryIntervalSeconds(ProductionIotConstants.SESSION_EXPIRY_INTERVAL_SECONDS);

            logger.info("MQTT connecting endpoint=" + params.endpoint + " clientId=" + params.clientId
                    + " authorizer=" + params.authorizerName);
            Mqtt5Client newClient = AwsIotMqtt5ClientBuilder
                    .newDirectMqttBuilderWithCustomAuth(params.endpoint, customAuth)
                    .withConnectProperties(connectPacket)
                    .withSessionBehavior(Mqtt5ClientOptions.ClientSessionBehavior.REJOIN_ALWAYS)
                    .withLifeCycleEvents(this)
                    .withPublishEvents(this)
                    .build();
            synchronized (lock) {
                client = newClient;
            }
            newClient.start();
        } catch (Exception e) {
            connecting.set(false);
            connected.set(false);
            logger.fail("MQTT connect threw: " + e.getMessage());
            future.complete(false);
        }
        return future;
    }

    public void disconnect() {
        logger.info("MQTT disconnect requested");
        stopQuietly();
        connected.set(false);
        connecting.set(false);
        Listener l = listener;
        if (l != null) {
            l.onDisconnect();
        }
    }

    @NonNull
    public CompletableFuture<Boolean> subscribe(@NonNull String topic, int qos) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Mqtt5Client mqtt = client;
        if (mqtt == null || !connected.get()) {
            future.complete(false);
            return future;
        }
        try {
            logger.info("MQTT subscribe topic=" + topic + " qos=" + qos);
            QOS mqttQos = qos == 0 ? QOS.AT_MOST_ONCE : QOS.AT_LEAST_ONCE;
            SubscribePacket packet = new SubscribePacket.SubscribePacketBuilder(topic, mqttQos).build();
            mqtt.subscribe(packet).whenComplete((subAck, error) -> {
                if (error != null) {
                    logger.fail("SUBACK error " + topic + ": " + error.getMessage());
                    future.complete(false);
                    return;
                }
                boolean granted = false;
                if (subAck != null && subAck.getReasonCodes() != null) {
                    for (SubAckPacket.SubAckReasonCode code : subAck.getReasonCodes()) {
                        if (code.name().startsWith("GRANTED_")) {
                            granted = true;
                            logger.pass("Subscription confirmed " + topic + " [" + code.name() + "]");
                        } else {
                            logger.fail("Subscription rejected " + topic + " [" + code.name() + "]");
                        }
                    }
                }
                future.complete(granted);
            });
        } catch (Exception e) {
            logger.fail("Subscribe failed: " + e.getMessage());
            future.complete(false);
        }
        return future;
    }

    public boolean publish(@NonNull String topic, @NonNull String payload) {
        Mqtt5Client mqtt = client;
        if (mqtt == null || !connected.get()) {
            logger.fail("Publish skipped — MQTT not connected");
            return false;
        }
        try {
            PublishPacket packet = new PublishPacket.PublishPacketBuilder(
                    topic, QOS.AT_LEAST_ONCE, payload.getBytes(StandardCharsets.UTF_8)).build();
            mqtt.publish(packet);
            logger.pass("Published topic=" + topic + " bytes=" + payload.length());
            return true;
        } catch (Exception e) {
            logger.fail("Publish failed: " + e.getMessage());
            return false;
        }
    }

    private void stopQuietly() {
        Mqtt5Client old;
        synchronized (lock) {
            old = client;
            client = null;
        }
        if (old != null) {
            try {
                old.stop();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onAttemptingConnect(Mqtt5Client mqtt5Client, OnAttemptingConnectReturn onAttemptingConnectReturn) {
        connecting.set(true);
        connected.set(false);
        logger.info("MQTT attempting connect");
    }

    @Override
    public void onConnectionSuccess(Mqtt5Client mqtt5Client, OnConnectionSuccessReturn onConnectionSuccessReturn) {
        connected.set(true);
        connecting.set(false);
        logger.pass("MQTT connected");
        completeConnect(true);
        Listener l = listener;
        if (l != null) {
            l.onConnect();
        }
    }

    @Override
    public void onConnectionFailure(Mqtt5Client mqtt5Client, OnConnectionFailureReturn onConnectionFailureReturn) {
        connected.set(false);
        connecting.set(false);
        String reason = "MQTT connection failed";
        if (onConnectionFailureReturn != null && onConnectionFailureReturn.getConnAckPacket() != null) {
            reason = reason + " " + onConnectionFailureReturn.getConnAckPacket().getReasonCode();
        }
        logger.fail(reason);
        completeConnect(false);
        Listener l = listener;
        if (l != null) {
            l.onError(reason);
        }
    }

    @Override
    public void onDisconnection(Mqtt5Client mqtt5Client, OnDisconnectionReturn onDisconnectionReturn) {
        connected.set(false);
        connecting.set(false);
        logger.info("MQTT disconnected");
        Listener l = listener;
        if (l != null) {
            l.onDisconnect();
        }
    }

    @Override
    public void onStopped(Mqtt5Client mqtt5Client, OnStoppedReturn onStoppedReturn) {
        connected.set(false);
        connecting.set(false);
        synchronized (lock) {
            if (client == mqtt5Client) {
                client = null;
            }
        }
        logger.info("MQTT client stopped");
    }

    @Override
    public void onMessageReceived(Mqtt5Client mqtt5Client, PublishReturn publishReturn) {
        PublishPacket packet = publishReturn != null ? publishReturn.getPublishPacket() : null;
        if (packet == null) {
            return;
        }
        String topic = packet.getTopic();
        byte[] bytes = packet.getPayload();
        String payload = bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
        logger.info("MQTT message topic=" + topic + " bytes=" + payload.length());
        Listener l = listener;
        if (l != null) {
            l.onMessage(topic == null ? "" : topic, payload);
        }
    }

    private void completeConnect(boolean ok) {
        CompletableFuture<Boolean> future;
        synchronized (lock) {
            future = connectFuture;
            connectFuture = null;
        }
        if (future != null && !future.isDone()) {
            future.complete(ok);
        }
    }

    public static final class MqttConnectParams {
        public final String endpoint;
        public final String clientId;
        public final String authorizerName;
        public final String jwt;

        public MqttConnectParams(String endpoint, String clientId, String authorizerName, String jwt) {
            this.endpoint = endpoint;
            this.clientId = clientId;
            this.authorizerName = authorizerName;
            this.jwt = jwt;
        }
    }
}
