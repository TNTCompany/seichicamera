package android.net

import android.os.Parcel

fun createTestUri(uriString: String = "content://media/external/images/media/1"): Uri {
    return object : Uri() {
        override fun isHierarchical() = false
        override fun isRelative() = false
        override fun getScheme() = "content"
        override fun getSchemeSpecificPart() = uriString
        override fun getEncodedSchemeSpecificPart() = uriString
        override fun getAuthority() = "media"
        override fun getEncodedAuthority() = "media"
        override fun getUserInfo() = null
        override fun getEncodedUserInfo() = null
        override fun getHost() = null
        override fun getPort() = -1
        override fun getPath() = "/1"
        override fun getEncodedPath() = "/1"
        override fun getQuery() = null
        override fun getEncodedQuery() = null
        override fun getFragment() = null
        override fun getEncodedFragment() = null
        override fun getPathSegments() = emptyList<String>()
        override fun getLastPathSegment() = "1"
        override fun buildUpon() = null
        override fun describeContents() = 0
        override fun writeToParcel(dest: Parcel, flags: Int) {}
        override fun toString() = uriString
        override fun compareTo(other: Uri?) = 0
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Uri) return false
            return this.toString() == other.toString()
        }
        override fun hashCode(): Int = toString().hashCode()
    }
}
