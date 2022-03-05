package com.riyazuddin.zing.other

object Constants {

    /**
     * Notification Type
     */
    const val NOTIFICATION_ID = 1
    const val CHAT_CHANNEL_ID = "chat_channel_id"
    const val NORMAL_NOTIFICATION_CHANNEL_ID = "normal_notification_channel_id"
    const val CHAT_TYPE = "CHAT_TYPE"
    const val FOLLOW_TYPE = "FOLLOW_TYPE"
    const val POST_LIKE_TYPE = "POST_LIKE_TYPE"
    const val COMMENT_TYPE = "COMMENT_TYPE"
    const val TYPE = "type"
    const val KEY = "key"
    const val CID = "cid"

    const val DEFAULT_PROFILE_PICTURE_URL =
        "https://firebasestorage.googleapis.com/v0/b/zing515.appspot.com/o/img_avatar.png?alt=media&token=b40984fc-155d-4acc-b031-a38076a98628"
    const val STREAM_TOKEN_API_URL = "https://asia-south1-zing515.cloudfunctions.net"

    const val ENCRYPTED_SHARED_PREF_NAME = "enc_shared_pref"
    const val STREAM_TOKEN_KEY = "stream_token_key"
    const val NO_TOKEN = "no_token"

    const val ALGOLIA_USER_SEARCH_INDEX = "user_search"

    const val USERS_COLLECTION = "users"
    const val USERS_METADATA_COLLECTION = "usersMetadata"
    const val USERS_STAT_COLLECTION = "usersStat"
    const val POSTS_COLLECTION = "posts"
    const val FEEDS_COLLECTION = "feeds"
    const val FEED_COLLECTION = "feed"
    const val COMMENTS_COLLECTION = "comments"
    const val FOLLOWING_COLLECTION = "following"
    const val FOLLOWERS_COLLECTION = "followers"
    const val POST_LIKES_COLLECTION = "postLikes"
    const val FOLLOWING_REQUESTS_COLLECTION = "followingRequests"
    const val FOLLOWER_REQUESTS_COLLECTION = "followerRequests"
    const val BUG_REPORT_COLLECTION = "bugReport"

    /**
     * For User Stat
     */
    const val STATE = "state"
    const val LAST_SEEN = "lastSeen"
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
    const val PRIVACY = "privacy"

    const val POST_ID = "postId"
    const val POSTED_BY = "postedBy"
    const val LIKED_BY = "likedBy"
    const val LIKE_COUNT = "likeCount"

    /**
     * For Message Class
     */
    const val MESSAGE_ID = "messageId"
    const val DATE = "date"

    const val CHAT_MESSAGE_PAGE_LIMIT = 10L
    const val POST_PAGE_SIZE = 5
    const val COMMENT_PAGE_SIZE = 10
    const val NEW_CHAT_PAGE_SIZE = 10
    const val USER_PAGE_SIZE = 10
    const val FEED_PAGE_SIZE = 3

    const val VALID = "VALID"
    const val INVALID = "INVALID"
    const val MAX_NAME = 20
    const val MIN_PASSWORD = 8
    const val MAX_PASSWORD = 20
    const val MIN_USERNAME = 3
    const val MAX_USERNAME = 15

    const val SEARCH_TIME_DELAY = 1000L

    const val ONLINE = "Online"
    const val OFFLINE = "Offline"

    const val PUBLIC = "public"
    const val PRIVATE = "private"

    const val NO_USER_DOCUMENT = "NO_USER_DOCUMENT"

    //For Following Request
    const val REQUESTED_TO_UIDS = "requestedToUids"
    const val REQUESTED_UIDS = "requestedUids"

}