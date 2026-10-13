package com.untis.homework

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.json.JSONObject
import org.junit.Test

class HomeworkJsonCodecTest {
  @Test
  fun parsesChildrenHomeworkAndStatusFields() {
    val fullTaskText = """
      Read the whole worksheet before class.
      Answer questions 1 through 8 in full sentences.
      Bring the signed page and note anything unclear for discussion.
      This final instruction must remain visible in the homework card.
    """.trimIndent()
    val payload = HomeworkJsonCodec.parse(
      """
      {
        "ok": true,
        "fetchedAt": "2026-05-20T10:00:00",
        "window": {"start": "2026-05-06", "end": "2026-06-10"},
        "children": {
          "student-two": {
            "displayName": "Example Two",
            "homeworks": [
              {
                "id": 20,
                "subject": "E",
                "subjectLongName": "English",
                "teacher": "TEA",
                "postedDate": "2026-05-18",
                "dueDate": "2026-05-22",
                "dueDateLabel": "Fr, 22.05.",
                "text": ${JSONObject.quote(fullTaskText)},
                "status": "open",
                "isPastDue": false,
                "isArchived": false,
                "attachments": ["worksheet.pdf"]
              }
            ]
          },
          "student-one": {
            "displayName": "Example One",
            "homeworks": [
              {"id": 11, "subject": "M", "text": "Finished", "status": "completed", "isArchived": true},
              {"id": 12, "subject": "D", "text": "Missed", "status": "not_done", "isPastDue": true, "isArchived": true}
            ]
          }
        }
      }
      """.trimIndent()
    )

    assertEquals("2026-05-06", payload.windowStart)
    assertEquals(listOf("student-one", "student-two"), payload.children.map { it.key })
    val studentOne = payload.children.single { it.key == "student-one" }
    val studentTwo = payload.children.single { it.key == "student-two" }
    assertEquals(fullTaskText, studentTwo.homeworks.single().text)
    assertEquals(HomeworkStatus.Completed, studentOne.homeworks[0].status)
    assertEquals(HomeworkStatus.NotDone, studentOne.homeworks[1].status)
    assertTrue(studentOne.homeworks[1].isPastDue)
    assertFalse(studentTwo.homeworks.single().isArchived)
  }


  @Test
  fun updatesCachedHomeworkStatusForLocalPersistence() {
    val cached = """
      {
        "ok": true,
        "fetchedAt": "2026-05-20T10:00:00",
        "window": {"start": "2026-05-06", "end": "2026-06-10"},
        "children": {
          "student-one": {
            "displayName": "Example One",
            "homeworks": [
              {"id": 11, "subject": "M", "text": "Task", "status": "open", "isArchived": false}
            ]
          }
        }
      }
      """.trimIndent()

    val completed = HomeworkJsonCodec.updateHomeworkStatus(cached, "student-one", "11", HomeworkStatus.Completed)
    val completedItem = JSONObject(completed)
      .getJSONObject("children")
      .getJSONObject("student-one")
      .getJSONArray("homeworks")
      .getJSONObject(0)
    assertEquals("completed", completedItem.getString("status"))
    assertTrue(completedItem.getBoolean("isArchived"))

    val reopened = HomeworkJsonCodec.updateHomeworkStatus(completed, "student-one", "11", HomeworkStatus.Open)
    val reopenedItem = JSONObject(reopened)
      .getJSONObject("children")
      .getJSONObject("student-one")
      .getJSONArray("homeworks")
      .getJSONObject(0)
    assertEquals("open", reopenedItem.getString("status"))
    assertFalse(reopenedItem.getBoolean("isArchived"))
  }



}
