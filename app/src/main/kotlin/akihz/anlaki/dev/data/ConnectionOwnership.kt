package akihz.anlaki.dev.data

/** Tracks independent owners of one shared service connection. */
class ConnectionOwnership {
    private val owners = mutableSetOf<String>()

    /**
     * Adds [owner].
     *
     * @return true when this is the first owner and a connection should be started
     */
    fun acquire(owner: String): Boolean {
        val added = owners.add(owner)
        return added && owners.size == 1
    }

    /**
     * Removes [owner].
     *
     * @return true when an existing final owner was removed and disconnect is safe
     */
    fun release(owner: String): Boolean {
        val removed = owners.remove(owner)
        return removed && owners.isEmpty()
    }

    /** Whether at least one component currently owns the connection. */
    fun isActive(): Boolean = owners.isNotEmpty()
}
