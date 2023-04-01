package com.lesa_humdet;

public class Conf {
    private String shared_pref_name = "settings_gz";
    //private String domen = "http://lesa01.mircloud.host/api/";
    private String domen = "http://node142496-env-8413235.mircloud.host/api/";
//<!--android:networkSecurityConfig="@xml/network_security_config"-->

    private final String LANG = "lang";
    private final int RU = 1;
    private final int EN = 2;
    private final int AR = 3;



    public String getShared_pref_name() {
        return shared_pref_name;
    }

    public void setShared_pref_name(String shared_pref_name) {
        this.shared_pref_name = shared_pref_name;
    }

    public String getLANG() {
        return LANG;
    }

    public int getRU() {
        return RU;
    }

    public int getEN() {
        return EN;
    }

    public int getAR() {
        return AR;
    }

    public String getDomen() {
        return domen;
    }

    public void setDomen(String domen) {
        this.domen = domen;
    }
}
