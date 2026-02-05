package com.quezic.data.remote

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

/**
 * Initializes the NewPipe Extractor library.
 * Must be called before using any extractor functionality.
 */
object NewPipeInit {
    
    private var initialized = false
    
    @Synchronized
    fun init() {
        if (!initialized) {
            NewPipe.init(
                NewPipeDownloader.getInstance(),
                Localization.DEFAULT,
                ContentCountry.DEFAULT
            )
            initialized = true
        }
    }
    
    fun isInitialized(): Boolean = initialized
}
