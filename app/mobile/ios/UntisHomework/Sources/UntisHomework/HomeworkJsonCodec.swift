import Foundation

enum HomeworkJsonCodec {
    static func parse(_ rawJson: String) throws -> HomeworkPayload {
        let data = Data(rawJson.utf8)
        let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
        let window = root["window"] as? [String: Any] ?? [:]
        let childrenObject = root["children"] as? [String: Any] ?? [:]
        let children = childrenObject.keys.sorted().map { key -> ChildHomeworks in
            let child = childrenObject[key] as? [String: Any] ?? [:]
            let rawHomeworks = child["homeworks"] as? [[String: Any]] ?? []
            return ChildHomeworks(
                key: key,
                displayName: string(child["displayName"], fallback: key),
                homeworks: rawHomeworks.map { item in
                    HomeworkItem(
                        id: string(item["id"]),
                        subject: string(item["subject"]),
                        subjectLongName: string(item["subjectLongName"]),
                        teacher: string(item["teacher"]),
                        postedDate: string(item["postedDate"]),
                        dueDate: string(item["dueDate"]),
                        dueDateLabel: string(item["dueDateLabel"]),
                        text: string(item["text"]),
                        status: parseStatus(string(item["status"])),
                        isPastDue: item["isPastDue"] as? Bool ?? false,
                        isArchived: item["isArchived"] as? Bool ?? false,
                        attachments: (item["attachments"] as? [Any] ?? []).compactMap { raw in
                            let text = string(raw)
                            return text.isEmpty ? nil : text
                        }
                    )
                }
            )
        }
        return HomeworkPayload(
            fetchedAt: string(root["fetchedAt"]),
            windowStart: string(window["start"]),
            windowEnd: string(window["end"]),
            children: children
        )
    }

    static func updateHomeworkStatus(
        rawJson: String,
        childKey: String,
        homeworkId: String,
        status: HomeworkStatus
    ) -> String {
        guard let data = rawJson.data(using: .utf8),
              var root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              var children = root["children"] as? [String: Any],
              var child = children[childKey] as? [String: Any],
              var homeworks = child["homeworks"] as? [[String: Any]]
        else {
            return rawJson
        }

        guard let index = homeworks.firstIndex(where: { string($0["id"]) == homeworkId }) else {
            return rawJson
        }

        homeworks[index]["status"] = status.jsonValue
        homeworks[index]["isArchived"] = status != .open
        child["homeworks"] = homeworks
        children[childKey] = child
        root["children"] = children

        guard let updated = try? JSONSerialization.data(withJSONObject: root),
              let text = String(data: updated, encoding: .utf8)
        else {
            return rawJson
        }
        return text
    }

    private static func parseStatus(_ value: String) -> HomeworkStatus {
        switch value {
        case "completed": .completed
        case "not_done": .notDone
        default: .open
        }
    }
}

private extension HomeworkStatus {
    var jsonValue: String {
        switch self {
        case .open: "open"
        case .completed: "completed"
        case .notDone: "not_done"
        }
    }
}

func string(_ value: Any?, fallback: String = "") -> String {
    switch value {
    case let value as String:
        value
    case let value as NSNumber:
        value.stringValue
    case let value?:
        String(describing: value)
    case nil:
        fallback
    }
}

func parseUntisDate(_ value: Any?) -> Date? {
    let text = string(value)
    guard text.count >= 8 else { return nil }
    return DateFormatter.untisDate.date(from: String(text.prefix(8)))
}

func yyyymmdd(_ value: Date) -> Int {
    Int(DateFormatter.untisDate.string(from: value)) ?? 0
}

func displayDueDate(_ value: Date?) -> String {
    guard let value else { return "" }
    return DateFormatter.displayDueDate.string(from: value)
}

func normalizeUntisHomeworks(
    data: [String: Any],
    start: Date,
    end: Date,
    childrenList: [UntisChild],
    completedIds: Set<String>,
    subjects: [String: [String: Any]],
    today: Date = Date()
) -> [String: Any] {
    let homeworks = byId(data["homeworks"] as? [[String: Any]])
    let teachers = byId(data["teachers"] as? [[String: Any]])
    let lessons = byId(data["lessons"] as? [[String: Any]])
    let records = data["records"] as? [[String: Any]] ?? []
    var children: [String: Any] = [:]
    let calendar = Calendar.current
    let todayStart = calendar.startOfDay(for: today)

    for child in childrenList {
        var rows: [[String: Any]] = []
        for record in records {
            guard containsInt(record["elementIds"], value: child.personId),
                  let homeworkIdRaw = record["homeworkId"]
            else {
                continue
            }
            let homeworkId = string(homeworkIdRaw)
            guard let homework = homeworks[homeworkId] else { continue }
            let dueDate = parseUntisDate(homework["dueDate"])
            let postedDate = parseUntisDate(homework["date"])
            let lesson = lessons[string(homework["lessonId"])] ?? [:]
            let teacher = teachers[string(record["teacherId"])] ?? [:]
            let subject = string(lesson["subject"])
            let subjectMeta = subjects[subject.uppercased()]
            let stateKey = "\(child.key):\(homeworkId)"
            let completed = completedIds.contains(stateKey)
            let pastDue = dueDate.map { calendar.startOfDay(for: $0) < todayStart } ?? false
            let status = completed ? "completed" : (pastDue ? "not_done" : "open")
            rows.append([
                "id": homeworkId,
                "subject": subject,
                "subjectLongName": string(subjectMeta?["longName"]),
                "teacher": string(teacher["name"]),
                "postedDate": postedDate.map { DateFormatter.isoDate.string(from: $0) } ?? "",
                "dueDate": dueDate.map { DateFormatter.isoDate.string(from: $0) } ?? "",
                "dueDateLabel": displayDueDate(dueDate),
                "text": string(homework["text"]),
                "status": status,
                "isPastDue": pastDue,
                "isArchived": status != "open",
                "attachments": attachmentNames(homework["attachments"])
            ])
        }
        rows.sort {
            let leftDue = string($0["dueDate"], fallback: "9999-12-31").isEmpty ? "9999-12-31" : string($0["dueDate"])
            let rightDue = string($1["dueDate"], fallback: "9999-12-31").isEmpty ? "9999-12-31" : string($1["dueDate"])
            return [leftDue, string($0["subject"]), string($0["teacher"]), string($0["id"])]
                .lexicographicallyPrecedes([rightDue, string($1["subject"]), string($1["teacher"]), string($1["id"])])
        }
        children[child.key] = [
            "displayName": child.displayName,
            "personId": child.personId,
            "homeworks": rows
        ]
    }

    return [
        "ok": true,
        "schema_version": "untis_homeworks_v1",
        "fetchedAt": ISO8601DateFormatter().string(from: Date()),
        "window": [
            "start": DateFormatter.isoDate.string(from: start),
            "end": DateFormatter.isoDate.string(from: end)
        ],
        "children": children
    ]
}

func makeHomeworkPayloadJson(_ payload: [String: Any]) throws -> String {
    let data = try JSONSerialization.data(withJSONObject: payload)
    return String(data: data, encoding: .utf8) ?? "{}"
}

private func byId(_ items: [[String: Any]]?) -> [String: [String: Any]] {
    (items ?? []).reduce(into: [:]) { result, item in
        let id = string(item["id"])
        if !id.isEmpty {
            result[id] = item
        }
    }
}

private func containsInt(_ value: Any?, value expected: Int) -> Bool {
    guard let array = value as? [Any] else { return false }
    return array.contains { item in
        if let int = item as? Int { return int == expected }
        if let number = item as? NSNumber { return number.intValue == expected }
        return false
    }
}

private func attachmentNames(_ value: Any?) -> [String] {
    guard let attachments = value as? [[String: Any]] else { return [] }
    return attachments.compactMap { attachment in
        let name = string(attachment["name"])
        return name.isEmpty ? nil : name
    }
}

extension DateFormatter {
    static let untisDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyyMMdd"
        return formatter
    }()

    static let isoDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    static let displayDueDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "E, dd.MM."
        return formatter
    }()
}

extension HomeworkItem {
    var subjectKey: String {
        let stripped = subject.trimmingCharacters(in: .whitespacesAndNewlines)
            .uppercased()
            .replacingOccurrences(of: "[0-9]", with: "", options: .regularExpression)
        if !stripped.isEmpty {
            return stripped
        }
        let longName = subjectLongName.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if !longName.isEmpty {
            return String(longName.prefix(16))
        }
        return "?"
    }

    var subjectTitle: String {
        subjectLongName.isEmpty ? (subject.isEmpty ? "Subject" : subject) : "\(subject) - \(subjectLongName)"
    }

    var dueText: String {
        if let date = DateFormatter.isoDate.date(from: dueDate) {
            return "Due \(DateFormatter.displayDueDate.string(from: date))"
        }
        if !dueDateLabel.isEmpty {
            return "Due \(dueDateLabel)"
        }
        return "No due date"
    }
}

extension Array where Element == HomeworkItem {
    func filtered(_ filter: HomeworkFilter) -> [HomeworkItem] {
        switch filter {
        case .open:
            return self.filter { $0.status == .open }
        case .archive:
            return self.filter { $0.status != .open }
        case .all:
            return self
        }
    }
}

func swipeCompletionTarget(delta: CGFloat, status: HomeworkStatus, threshold: CGFloat = 82) -> Bool? {
    if delta > threshold, status != .completed {
        return true
    }
    if delta < -threshold, status == .completed {
        return false
    }
    return nil
}

extension String {
    var shortChildName: String {
        split(whereSeparator: \.isWhitespace).last.map(String.init) ?? self
    }
}
