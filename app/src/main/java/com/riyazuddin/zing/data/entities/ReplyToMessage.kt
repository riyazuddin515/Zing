package com.riyazuddin.zing.data.entities

data class ReplyToMessage(
    var messageCreatorName: String = "",
    var replyToMessage: String = "",
    var replyToUrl: String = "",
    var replyToType: String = ""
)