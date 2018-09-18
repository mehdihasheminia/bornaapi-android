package com.bornaapp.android;

import java.util.Date;

/**
 * Created by Hashemi on 17/02/2018.
 */

public class GoogleSaveData {
    public int coins = 10;
    public Date lastModified = new Date();
    public long gamesPlayed = 0;
    public long gamesWon = 0;
    public int skins = 0;

    public void setDefaultValues(){
        coins = 10;
        lastModified = new Date();
        gamesPlayed = 0;
        gamesWon = 0;
        skins = 0;
    }
}
