package co.poynt.iot.companion.shared.iot.phase2;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

/**
 * Same mothership Discover JSON model as production {@code DiscoveredIoTServices}.
 */
public class DiscoveredIoTServices {

    @SerializedName("iot")
    private IoTConfig iot;

    public boolean hasIotConfig() {
        return iot != null;
    }

    public String getIotEndpoint() {
        return iot != null ? iot.endpoint : null;
    }

    public String getRegion() {
        return iot != null ? iot.region : null;
    }

    public String getAuthorizerName() {
        return iot != null ? iot.authorizerName : null;
    }

    public JobConfig getJobConfig() {
        return iot != null ? iot.jobConfig : null;
    }

    public static class IoTConfig {
        @SerializedName("endpoint")
        String endpoint;
        @SerializedName("region")
        String region;
        @SerializedName("authorizer_name")
        String authorizerName;
        @SerializedName("job_config")
        JobConfig jobConfig;
    }

    public static class JobConfig {
        @SerializedName("polling_interval_hrs")
        int pollingIntervalHrs;
        @SerializedName("mqtt_topics")
        MqttTopics mqttTopics;

        public MqttTopics getMqttTopics() {
            return mqttTopics;
        }
    }

    public static class MqttTopics {
        @SerializedName("jobs")
        @JsonAdapter(MqttTopicConfig.Deserializer.class)
        MqttTopicConfig jobs;
        @SerializedName("maintenance")
        @JsonAdapter(MqttTopicConfig.Deserializer.class)
        MqttTopicConfig maintenance;

        public MqttTopicConfig getJobs() {
            return jobs;
        }

        public MqttTopicConfig getMaintenance() {
            return maintenance;
        }
    }

    public static class MqttTopicConfig {
        @SerializedName("topic")
        String topic;
        @SerializedName("qos")
        Integer qos;

        MqttTopicConfig(String topic, Integer qos) {
            this.topic = topic;
            this.qos = qos;
        }

        public String getTopic() {
            return topic;
        }

        public int getQos() {
            if (qos == null || qos < 0 || qos > 2) {
                return ProductionIotConstants.DEFAULT_QOS;
            }
            return qos;
        }

        public static class Deserializer implements JsonDeserializer<MqttTopicConfig> {
            @Override
            public MqttTopicConfig deserialize(JsonElement json, Type typeOfT,
                                              JsonDeserializationContext context) throws JsonParseException {
                if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                    return new MqttTopicConfig(json.getAsString(), ProductionIotConstants.DEFAULT_QOS);
                }
                if (json.isJsonObject()) {
                    JsonObject obj = json.getAsJsonObject();
                    String t = obj.has("topic") ? obj.get("topic").getAsString() : null;
                    Integer q = obj.has("qos") ? obj.get("qos").getAsInt() : null;
                    return new MqttTopicConfig(t, q);
                }
                throw new JsonParseException("Unexpected JSON for MqttTopicConfig: " + json);
            }
        }
    }

    @Override
    public String toString() {
        return "DiscoveredIoTServices{endpoint='" + getIotEndpoint()
                + "', authorizer='" + getAuthorizerName() + "'}";
    }
}
