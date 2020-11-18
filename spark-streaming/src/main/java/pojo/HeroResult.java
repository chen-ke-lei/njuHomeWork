package pojo;

import java.io.Serializable;

public class HeroResult implements Serializable {
    private String name;
    private String id;
    private int playNum;
    private int winNum;
    private double winRate;
    private String updateTime="00000000";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPlayNum() {
        return playNum;
    }

    public void setPlayNum(int playNum) {
        this.playNum = playNum;
    }

    public int getWinNum() {
        return winNum;
    }

    public void setWinNum(int winNum) {
        this.winNum = winNum;
    }

    public double getWinRate() {
        return winRate;
    }

    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "HeroResult{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", playNum=" + playNum +
                ", winNum=" + winNum +
                ", winRate=" + winRate +
                ", updateTime='" + updateTime + '\'' +
                '}';
    }

    public HeroResult() {
    }
}
