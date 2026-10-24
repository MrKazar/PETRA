package com.example.porjectpetra.ui.routine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.porjectpetra.data.model.DailyPlanner
import com.example.porjectpetra.data.model.PlannedTask
import com.example.porjectpetra.data.model.TaskStatus
import com.example.porjectpetra.ui.components.TaskCheckRow
import com.example.porjectpetra.ui.components.TaskVisualState

fun TaskStatus?.toVisualState(): TaskVisualState = when (this) {
    TaskStatus.DONE -> TaskVisualState.DONE
    TaskStatus.SKIPPED -> TaskVisualState.SKIPPED
    TaskStatus.POSTPONED -> TaskVisualState.POSTPONED
    null -> TaskVisualState.PENDING
}

/** Un appui coche / décoche ; une tâche sautée ou reportée redevient « à faire ». */
fun TaskStatus?.toggled(): TaskStatus? = if (this == null) TaskStatus.DONE else null

/** Ligne de tâche du jour, partagée par l'accueil et les routines. */
@Composable
fun PlannedTaskRow(
    item: PlannedTask,
    onSetStatus: (PlannedTask, TaskStatus?) -> Unit,
    modifier: Modifier = Modifier,
    showTrigger: Boolean = false,
    onEdit: ((PlannedTask) -> Unit)? = null
) {
    val task = item.task
    val details = buildList {
        if (item.carriedOver) add("Reportée d'hier")
        if (showTrigger && task.routineId == null) {
            add(DailyPlanner.triggerLabel(task.triggerType, task.scheduledMinutes, task.triggerEvent))
        }
        if (task.estimatedMinutes > 0) add("${task.estimatedMinutes} min")
    }.joinToString(" · ").ifEmpty { null }

    val menu = buildList<Pair<String, () -> Unit>> {
        if (item.status != null) add("Remettre à faire" to { onSetStatus(item, null) })
        if (item.status != TaskStatus.SKIPPED) add("Sauter aujourd'hui" to { onSetStatus(item, TaskStatus.SKIPPED) })
        if (item.status != TaskStatus.POSTPONED) add("Reporter à demain" to { onSetStatus(item, TaskStatus.POSTPONED) })
        if (onEdit != null) add("Modifier" to { onEdit(item) })
    }

    TaskCheckRow(
        title = task.title,
        state = item.status.toVisualState(),
        onToggle = { onSetStatus(item, item.status.toggled()) },
        supportingText = details,
        menuItems = menu,
        modifier = modifier
    )
}
