package pojo;

import java.io.Serializable;

public class SiteMessage implements Serializable {
    double damageDealt;
    String site;
    double heal;
    double damageTaken;
    double totalPoint;
    String updateTime = "00000000";

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public double getDamageDealt() {
        return damageDealt;
    }

    public void setDamageDealt(double damageDealt) {
        this.damageDealt = damageDealt;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public double getHeal() {
        return heal;
    }

    public void setHeal(double heal) {
        this.heal = heal;
    }

    public double getDamageTaken() {
        return damageTaken;
    }

    public void setDamageTaken(double damageTaken) {
        this.damageTaken = damageTaken;
    }

    public double getTotalPoint() {
        return totalPoint;
    }

    public void calTotalPoint() {
        this.totalPoint = this.damageDealt + this.heal + this.damageTaken;
    }

    public void update(SiteMessage siteMessage) {
        this.setHeal(siteMessage.getHeal() + this.getHeal());
        this.setDamageTaken(siteMessage.getDamageTaken() + this.getDamageTaken());
        this.setDamageDealt(siteMessage.getDamageDealt() + this.getDamageDealt());
        this.setUpdateTime(siteMessage.getUpdateTime());
        this.calTotalPoint();
    }

}
