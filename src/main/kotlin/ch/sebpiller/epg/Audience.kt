package ch.sebpiller.epg

/**
 * Enumerates the different type of audience of an EPG.
 */
enum class Audience {
    /**
     * Ok for all public.
     */
    ALL,

    /**
     * Ok for 10+.
     */
    TEN,

    /**
     * Ok for 12+.
     */
    TWELVE,

    /**
     * Ok for 16+.
     */
    SIXTEEN,

    /**
     * Ok for 18+.
     */
    EIGHTEEN,

    /**
     * Ok for 7+.
     */
    SEVEN
}