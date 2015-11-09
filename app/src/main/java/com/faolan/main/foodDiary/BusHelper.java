package com.faolan.main.foodDiary;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public class BusHelper {
    private static Bus bus = null;

    public static Bus getBus(){
        if(bus == null){
            bus = new Bus(ThreadEnforcer.ANY);
        }
        return bus;
    }

}
