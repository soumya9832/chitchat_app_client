package com.example.chitchat.model

enum class TYPE {
    STORE_USER, // Indicates a message for storing a user on the server.
    START_CALL,
    CALL_RESPONSE,
    CREATE_OFFER, // Signifies the creation of an offer in WebRTC.
    OFFER_RECEIVED, // Indicates the reception of an offer in WebRTC.
    CREATE_ANSWER, // Signifies the creation of an answer in WebRTC.
    ANSWER_RECEIVED, // ndicates the reception of an answer in WebRTC.
    ICE_CANDIDATE, // Represents the exchange of ICE candidates in WebRTC.
    CALL_ENDED
}