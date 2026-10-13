import XCTest
@testable import UntisHomework

final class HomeworkJsonCodecTests: XCTestCase {
    func testParsesChildrenHomeworkAndStatusFields() throws {
        let fullTaskText = """
        Read the whole worksheet before class.
        Answer questions 1 through 8 in full sentences.
        Bring the signed page and note anything unclear for discussion.
        This final instruction must remain visible in the homework card.
        """
        let payload = try HomeworkJsonCodec.parse(
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
                      "text": \(Self.jsonQuote(fullTaskText)),
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
            """
        )

        XCTAssertEqual(payload.windowStart, "2026-05-06")
        XCTAssertEqual(payload.children.map(\.key), ["student-one", "student-two"])
        let studentOne = try XCTUnwrap(payload.children.first { $0.key == "student-one" })
        let studentTwo = try XCTUnwrap(payload.children.first { $0.key == "student-two" })
        XCTAssertEqual(studentTwo.homeworks.single?.text, fullTaskText)
        XCTAssertEqual(studentOne.homeworks[0].status, .completed)
        XCTAssertEqual(studentOne.homeworks[1].status, .notDone)
        XCTAssertTrue(studentOne.homeworks[1].isPastDue)
        XCTAssertFalse(try XCTUnwrap(studentTwo.homeworks.single).isArchived)
    }

    func testUpdatesCachedHomeworkStatusForLocalPersistence() throws {
        let cached = """
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
        """

        let completed = HomeworkJsonCodec.updateHomeworkStatus(rawJson: cached, childKey: "student-one", homeworkId: "11", status: .completed)
        let completedPayload = try HomeworkJsonCodec.parse(completed)
        XCTAssertEqual(completedPayload.children.single?.homeworks.single?.status, .completed)
        XCTAssertEqual(completedPayload.children.single?.homeworks.single?.isArchived, true)

        let reopened = HomeworkJsonCodec.updateHomeworkStatus(rawJson: completed, childKey: "student-one", homeworkId: "11", status: .open)
        let reopenedPayload = try HomeworkJsonCodec.parse(reopened)
        XCTAssertEqual(reopenedPayload.children.single?.homeworks.single?.status, .open)
        XCTAssertEqual(reopenedPayload.children.single?.homeworks.single?.isArchived, false)
    }

    func testExtractsAccountStudentProfilesFromSessionHtml() {
        let children = childrenFromSessionHTML(
            """
            <script>
            window.grupet = {
              "loginServiceConfig": {
                "user": {
                  "name": "review5@example.invalid",
                  "personId": 1000001,
                  "persons": [
                    {"id": 101, "type": 5, "displayName": "EXAMPLE One", "longName": "EXAMPLE", "foreName": "One"},
                    {"id": 202, "type": 5, "displayName": "EXAMPLE Two", "longName": "EXAMPLE", "foreName": "Two"}
                  ]
                }
              }
            };
            </script>
            """
        )

        XCTAssertEqual(children.map(\.displayName), ["EXAMPLE One", "EXAMPLE Two"])
        XCTAssertEqual(children.map(\.personId), [101, 202])
    }

    func testFallsBackToLoggedInStudentWhenNoParentPersonsExist() {
        let children = childrenFromSessionHTML(
            #"{"user":{"persons":[]}}"#,
            authResult: ["personType": 5, "personId": 42]
        )

        XCTAssertEqual(children.map(\.personId), [42])
    }

    func testDerivesStudentProfilesFromHomeworkRecordsWhenSessionOmitsChildren() {
        let children = childrenFromHomeworkData([
            "records": [
                ["elementIds": [101], "homeworkId": 1],
                ["elementIds": [202], "homeworkId": 2],
                ["elementIds": [101], "homeworkId": 3]
            ],
            "students": [
                ["id": 101, "displayName": "Example One"],
                ["id": 202, "displayName": "Example Two"]
            ]
        ])

        XCTAssertEqual(children.map(\.displayName), ["Example One", "Example Two"])
        XCTAssertEqual(children.map(\.personId), [101, 202])
    }

    private static func jsonQuote(_ value: String) -> String {
        let data = try! JSONSerialization.data(withJSONObject: [value])
        let text = String(data: data, encoding: .utf8)!
        return String(text.dropFirst().dropLast())
    }
}

final class AppearancePersistenceTests: XCTestCase {
    func testChildAppearanceRoundTripsNormalizedProfileAndBackgroundColor() {
        let raw = childAppearanceToJson(ChildAppearance(theme: "forest", bgColor: "#FFCC00"))
        let parsed = childAppearanceFromJson(raw)

        XCTAssertEqual(parsed.theme, "forest")
        XCTAssertEqual(parsed.bgColor, "#ffcc00")
        XCTAssertEqual(parsed.backgroundHex(), "#ffcc00")
    }

    func testCustomBackgroundDoesNotOverrideSystemDarkTheme() {
        let parsed = childAppearanceFromJson(#"{"theme":"forest","bgColor":"#FFCC00"}"#)

        XCTAssertEqual(parsed.backgroundHex(darkTheme: false), "#ffcc00")
        XCTAssertEqual(parsed.backgroundHex(darkTheme: true), "#14241a")
    }

    func testChildAppearanceFallsBackWhenStoredValuesAreInvalid() {
        let parsed = childAppearanceFromJson(#"{"theme":"unknown","bgColor":"red"}"#)

        XCTAssertEqual(parsed.theme, "ocean")
        XCTAssertEqual(parsed.bgColor, "")
        XCTAssertEqual(parsed.backgroundHex(), "#edf2f5")
    }

    func testChildAppearanceUsesDarkProfileBackgroundWhenSystemIsDark() {
        let parsed = childAppearanceFromJson(#"{"theme":"night"}"#)

        XCTAssertEqual(parsed.backgroundHex(darkTheme: false), "#e9edf3")
        XCTAssertEqual(parsed.backgroundHex(darkTheme: true), "#111827")
    }

    func testAccentSwitchesBetweenLightAndDarkProfiles() {
        let parsed = ChildAppearance(theme: "ocean")

        XCTAssertEqual(parsed.accentHex(darkTheme: false), "#0d747a")
        XCTAssertEqual(parsed.accentHex(darkTheme: true), "#6bd6d0")
    }

    func testSubjectStylesRoundTripValidChildScopedColorsOnly() {
        let raw = subjectStylesToJson([
            "student-one": ["M": "#FFCC00", "D": "red"],
            "student-two": ["E": "#66a06b"]
        ])

        XCTAssertEqual(
            subjectStylesFromJson(raw),
            [
                "student-one": ["M": "#ffcc00"],
                "student-two": ["E": "#66a06b"]
            ]
        )
    }

    func testHexColorNormalizerRequiresFullCssHexValues() {
        XCTAssertEqual(normalizeHexColor(" #0D747A "), "#0d747a")
        XCTAssertNil(normalizeHexColor("#123"))
        XCTAssertNil(normalizeHexColor("blue"))
    }
}

final class SwipeCompletionTargetTests: XCTestCase {
    func testRightSwipeCompletesNonCompletedHomeworkPastThreshold() {
        XCTAssertEqual(swipeCompletionTarget(delta: 83, status: .open), true)
        XCTAssertEqual(swipeCompletionTarget(delta: 83, status: .notDone), true)
    }

    func testLeftSwipeReopensOnlyCompletedHomeworkPastThreshold() {
        XCTAssertEqual(swipeCompletionTarget(delta: -83, status: .completed), false)
        XCTAssertNil(swipeCompletionTarget(delta: -83, status: .open))
        XCTAssertNil(swipeCompletionTarget(delta: -83, status: .notDone))
    }

    func testSmallDragsDoNotChangeCompletionState() {
        XCTAssertNil(swipeCompletionTarget(delta: 82, status: .open))
        XCTAssertNil(swipeCompletionTarget(delta: -82, status: .completed))
    }
}

final class SubjectColorRowsTests: XCTestCase {
    func testDuplicateSubjectKeysDoNotCrashSettingsRows() {
        let rows = subjectColorRows(for: [
            Self.homework(id: "1", subject: "M", subjectLongName: "Mathematics"),
            Self.homework(id: "2", subject: "M", subjectLongName: "Mathematics"),
            Self.homework(id: "3", subject: "E", subjectLongName: "English")
        ])

        XCTAssertEqual(rows.map(\.key), ["E", "M"])
        XCTAssertEqual(rows.map(\.label), ["E - English", "M - Mathematics"])
    }

    private static func homework(id: String, subject: String, subjectLongName: String) -> HomeworkItem {
        HomeworkItem(
            id: id,
            subject: subject,
            subjectLongName: subjectLongName,
            teacher: "",
            postedDate: "",
            dueDate: "",
            dueDateLabel: "",
            text: "",
            status: .open,
            isPastDue: false,
            isArchived: false,
            attachments: []
        )
    }
}

extension Array {
    var single: Element? {
        count == 1 ? self[0] : nil
    }
}
