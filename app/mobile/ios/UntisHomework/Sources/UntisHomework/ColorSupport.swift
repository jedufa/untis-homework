import Foundation
import SwiftUI

func normalizeHexColor(_ value: String?) -> String? {
    let text = (value ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
    guard text.range(of: "^#[0-9a-fA-F]{6}$", options: .regularExpression) != nil else {
        return nil
    }
    return text.lowercased()
}

func colorFromHex(_ value: String?) -> Color? {
    guard let normalized = normalizeHexColor(value) else { return nil }
    let scanner = Scanner(string: String(normalized.dropFirst()))
    var hex: UInt64 = 0
    guard scanner.scanHexInt64(&hex) else { return nil }
    return Color(
        red: Double((hex >> 16) & 0xff) / 255.0,
        green: Double((hex >> 8) & 0xff) / 255.0,
        blue: Double(hex & 0xff) / 255.0
    )
}

func childAppearanceFromJson(_ rawJson: String?) -> ChildAppearance {
    guard let data = rawJson?.data(using: .utf8),
          let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    else {
        return ChildAppearance()
    }
    let rawTheme = object["theme"] as? String ?? "ocean"
    let theme = themeProfiles.contains { $0.key == rawTheme } ? rawTheme : "ocean"
    return ChildAppearance(theme: theme, bgColor: normalizeHexColor(object["bgColor"] as? String) ?? "")
}

func childAppearanceToJson(_ appearance: ChildAppearance) -> String {
    let normalized = ChildAppearance(
        theme: themeProfiles.contains { $0.key == appearance.theme } ? appearance.theme : "ocean",
        bgColor: normalizeHexColor(appearance.bgColor) ?? ""
    )
    let data = (try? JSONEncoder().encode(normalized)) ?? Data("{}".utf8)
    return String(data: data, encoding: .utf8) ?? "{}"
}

func subjectStylesToJson(_ styles: [String: [String: String]]) -> String {
    var root: [String: [String: String]] = [:]
    for childKey in styles.keys.sorted() {
        let normalized = styles[childKey, default: [:]].keys.sorted().reduce(into: [String: String]()) { result, subjectKey in
            if let color = normalizeHexColor(styles[childKey]?[subjectKey]) {
                result[subjectKey] = color
            }
        }
        if !normalized.isEmpty {
            root[childKey] = normalized
        }
    }
    let data = (try? JSONSerialization.data(withJSONObject: root, options: [.sortedKeys])) ?? Data("{}".utf8)
    return String(data: data, encoding: .utf8) ?? "{}"
}

func subjectStylesFromJson(_ rawJson: String?) -> [String: [String: String]] {
    guard let data = rawJson?.data(using: .utf8),
          let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    else {
        return [:]
    }
    var result: [String: [String: String]] = [:]
    for (childKey, rawChild) in root {
        guard let rawStyles = rawChild as? [String: Any] else { continue }
        var styles: [String: String] = [:]
        for (subjectKey, rawColor) in rawStyles {
            if let color = normalizeHexColor(rawColor as? String) {
                styles[subjectKey] = color
            }
        }
        if !styles.isEmpty {
            result[childKey] = styles
        }
    }
    return result
}
