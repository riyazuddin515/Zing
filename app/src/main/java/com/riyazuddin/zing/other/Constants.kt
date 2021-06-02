package com.riyazuddin.zing.other

object Constants {

    const val CHATTING_WITH = "chatting_with"
    const val NO_ONE = "NO_ONE"
    const val NOTIFICATION_ID = 1
    const val CHANNEL_ID = "chat_channel_id"

    const val DEFAULT_PROFILE_PICTURE_URL =
        "https://firebasestorage.googleapis.com/v0/b/zing515.appspot.com/o/img_avatar.png?alt=media&token=b40984fc-155d-4acc-b031-a38076a98628"

    const val USERS_COLLECTION = "users"
    const val USERS_STAT_COLLECTION = "usersStat"
    const val POSTS_COLLECTION = "posts"
    const val COMMENTS_COLLECTION = "comments"
    const val FOLLOWING_COLLECTION = "following"
    const val FOLLOWERS_COLLECTION = "followers"
    const val POST_LIKES_COLLECTION = "postLikes"
    const val CHATS_COLLECTION = "chats"
    const val MESSAGES_COLLECTION = "messages"

    /**
     * For User Stat
     */
    const val STATE = "state"
    const val  LAST_SEEN = "lastSeen"
    const val TOKEN = "token"
    /**
     * For User class
     */
    const val NAME = "name"
    const val UID = "uid"
    const val USERNAME = "username"
    const val PROFILE_PIC_URL = "profilePicUrl"
    const val BIO = "bio"
    const val FOLLOWING_COUNT = "followingCount"
    const val FOLLOWERS_COUNT = "followersCount"
    const val POST_COUNT = "postCount"

    /**
     * For Message Class
     */
    const val MESSAGE_ID = "messageId"
    const val MESSAGE = "message"
    const val URL = "url"
    const val TYPE = "type"
    const val DATE = "date"
    const val SENDER_AND_RECEIVER_UID = "senderAndReceiverUid"
    const val STATUS = "status"

    /**
     * For Last Message
     */
    const val SENDER_NAME = "senderName"
    const val SENDER_USERNAME = "senderUserName"
    const val SENDER_PROFILE_PIC_URL = "senderProfilePicUrl"
    const val RECEIVER_NAME = "receiverName"
    const val RECEIVER_USERNAME = "receiverUsername"
    const val RECEIVER_PROFILE_PIC_URL = "receiverProfilePicUrl"

    const val POST_PAGE_SIZE = 1
    const val COMMENT_PAGE_SIZE = 15
    const val NEW_CHAT_LIST_SIZE = 10

    const val VALID = "VALID"
    const val INVALID = "INVALID"
    const val MAX_NAME = 20
    const val MIN_PASSWORD = 8
    const val MAX_PASSWORD = 20
    const val VALID_USERNAME_MESSAGE = "Only a-z,A-z,0-9,_ are allowed. Should start with alphabet"
    const val MIN_USERNAME = 3
    const val MAX_USERNAME = 15

    const val SEARCH_TIME_DELAY = 500L

    const val TEXT = "TEXT"
    const val IMAGE = "IMAGE"

    const val SENDING = "SENDING"
    const val SENT = "SENT"
    const val DELIVERED = "DELIVERED"
    const val SEEN = "SEEN"

    const val NO_MORE_MESSAGES = "NO_MORE_MESSAGES"

    const val SUCCESS = "SUCCESS"
    const val FAILURE = "FAILURE"

}