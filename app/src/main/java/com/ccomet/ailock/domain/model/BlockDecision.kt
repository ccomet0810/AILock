package com.ccomet.ailock.domain.model

import com.ccomet.ailock.data.model.LockedAppConfig

sealed interface BlockDecision {
    data object Allow : BlockDecision

    data class ShowIntervention(
        val config: LockedAppConfig,
        val reason: String,
        val timeLimitExceeded: Boolean = false,
    ) : BlockDecision
}
