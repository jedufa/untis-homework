package com.untis.homework

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeCompletionTargetTest {
  @Test
  fun rightSwipeCompletesNonCompletedHomeworkPastThreshold() {
    assertEquals(true, swipeCompletionTarget(83f, HomeworkStatus.Open))
    assertEquals(true, swipeCompletionTarget(83f, HomeworkStatus.NotDone))
  }

  @Test
  fun leftSwipeReopensOnlyCompletedHomeworkPastThreshold() {
    assertEquals(false, swipeCompletionTarget(-83f, HomeworkStatus.Completed))
    assertNull(swipeCompletionTarget(-83f, HomeworkStatus.Open))
    assertNull(swipeCompletionTarget(-83f, HomeworkStatus.NotDone))
  }

  @Test
  fun smallDragsDoNotChangeCompletionState() {
    assertNull(swipeCompletionTarget(82f, HomeworkStatus.Open))
    assertNull(swipeCompletionTarget(-82f, HomeworkStatus.Completed))
  }
}
