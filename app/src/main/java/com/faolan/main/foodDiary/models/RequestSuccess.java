package com.faolan.main.foodDiary.models;

import com.google.api.services.calendar.model.Event;
import java.util.List;

public class RequestSuccess {
    public String message;
    public List<Event> events;

    public RequestSuccess(String message, List<Event> events){
        this.message = message;
        this.events = events;
    }
}
