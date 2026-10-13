import Foundation
import SwiftUI

struct ThemeProfile: Identifiable, Equatable {
    var id: String { key }
    let key: String
    let label: String
    let background: String
    let primary: String
    let secondary: String
    let late: String
    let done: String
    let darkBackground: String

    var darkAccent: String {
        switch key {
        case "ocean": "#6bd6d0"
        case "berry": "#f0a7cb"
        case "forest": "#94d99b"
        case "graphite": "#a9c0cf"
        case "sunrise": "#f0b477"
        case "mint": "#8ee3c2"
        case "paper": "#d6bf96"
        case "night": "#b8b4ff"
        default: primary
        }
    }
}

let themeProfiles: [ThemeProfile] = [
    ThemeProfile(key: "ocean", label: "Ocean", background: "#edf2f5", primary: "#0d747a", secondary: "#169590", late: "#af3649", done: "#4c7555", darkBackground: "#0f2024"),
    ThemeProfile(key: "berry", label: "Berry", background: "#f4eef4", primary: "#8d3d76", secondary: "#c05d8e", late: "#b2394a", done: "#497358", darkBackground: "#281923"),
    ThemeProfile(key: "forest", label: "Forest", background: "#eef3ef", primary: "#327052", secondary: "#66a06b", late: "#a84a3d", done: "#376d48", darkBackground: "#14241a"),
    ThemeProfile(key: "graphite", label: "Graphite", background: "#eef0f2", primary: "#3f5666", secondary: "#6c8493", late: "#a74451", done: "#4f7057", darkBackground: "#171d22"),
    ThemeProfile(key: "sunrise", label: "Sunrise", background: "#f7f1ea", primary: "#b25742", secondary: "#e09a53", late: "#a33c4a", done: "#4f7a59", darkBackground: "#281d17"),
    ThemeProfile(key: "mint", label: "Mint", background: "#edf6f1", primary: "#187b70", secondary: "#5bbf9d", late: "#aa4050", done: "#3f7b59", darkBackground: "#10231f"),
    ThemeProfile(key: "paper", label: "Paper", background: "#f4f2ec", primary: "#6d5a46", secondary: "#a48b65", late: "#a4404c", done: "#526f53", darkBackground: "#221f1a"),
    ThemeProfile(key: "night", label: "Night", background: "#e9edf3", primary: "#394b7a", secondary: "#7572b7", late: "#a63d5b", done: "#3f7463", darkBackground: "#111827"),
]

let colorSwatches = ["#ffcc00", "#66a06b", "#5bbf9d", "#8d3d76", "#e09a53", "#3f5666", "#ffffff"]

struct ChildAppearance: Equatable, Codable {
    var theme: String = "ocean"
    var bgColor: String = ""

    func themeProfile() -> ThemeProfile {
        themeProfiles.first { $0.key == theme } ?? themeProfiles[0]
    }

    func backgroundHex(darkTheme: Bool = false) -> String {
        if !darkTheme, let customBackground = normalizeHexColor(bgColor) {
            return customBackground
        }
        return darkTheme ? themeProfile().darkBackground : themeProfile().background
    }

    func accentHex(darkTheme: Bool = false) -> String {
        darkTheme ? themeProfile().darkAccent : themeProfile().primary
    }
}

struct HomeworkPayload: Equatable {
    let fetchedAt: String
    let windowStart: String
    let windowEnd: String
    let children: [ChildHomeworks]
}

struct ChildHomeworks: Identifiable, Equatable {
    var id: String { key }
    let key: String
    let displayName: String
    let homeworks: [HomeworkItem]
}

struct HomeworkItem: Identifiable, Equatable {
    let id: String
    let subject: String
    let subjectLongName: String
    let teacher: String
    let postedDate: String
    let dueDate: String
    let dueDateLabel: String
    let text: String
    let status: HomeworkStatus
    let isPastDue: Bool
    let isArchived: Bool
    let attachments: [String]
}

struct UntisChild: Equatable {
    let key: String
    let displayName: String
    let personId: Int
}

enum HomeworkStatus: String, Equatable {
    case open
    case completed
    case notDone

    var label: String {
        switch self {
        case .open: "Open"
        case .completed: "Done"
        case .notDone: "Missed"
        }
    }
}

enum HomeworkFilter: String, CaseIterable, Identifiable {
    case open = "Open"
    case archive = "Archive"
    case all = "All"

    var id: String { rawValue }
}

struct SubjectStyleTarget: Identifiable, Equatable {
    var id: String { "\(childKey):\(subjectKey)" }
    let childKey: String
    let subjectKey: String
    let label: String
}

