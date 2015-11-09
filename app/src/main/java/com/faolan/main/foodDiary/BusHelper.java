package com.faolan.main.foodDiary;

import com.squareup.otto.Bus;

public class BusHelper {
    private static Bus bus = null;

    public static Bus getBus(){
        if(bus == null){
            bus = new Bus();
        }
        return bus;
    }

}
