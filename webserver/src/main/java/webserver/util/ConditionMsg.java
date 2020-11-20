package webserver.util;

import lombok.Data;

@Data
public class ConditionMsg {
    private String start;
    private String end;
    private String hero;
    private String topic;
    private String groupId;

    @Override
    public String toString() {
        return "ConditionMsg{" +
                "start='" + start + '\'' +
                ", end='" + end + '\'' +
                ", hero='" + hero + '\'' +
                ", topic='" + topic + '\'' +
                ", groupId='" + groupId + '\'' +
                '}';
    }
}
