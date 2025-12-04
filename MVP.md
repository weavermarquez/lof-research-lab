# Rutlessly scoped MVP V1

## Core Data Model (MVP)

### Depot: user-actions

Source of truth for user activity.

| Field    | Type   | Example                                      |
|----------|--------|----------------------------------------------|
| user-id  | String | "guest" or frappe user id                    |
| action   | Enum   | :follow, :unfollow                           |
| work-url | String | "https://doi.org/..." or canonical URL       |

### PState: followed-works

Map of user to their followed works.

user-id: String → [work-url: String]

### PState: mentions-by-work

Map of work to its discovered mentions.

work-url: String → [Mention]

### Mention (object shape)

| Field    | Type   | Description                    |
|----------|--------|--------------------------------|
| url      | String | where the mention lives        |
| title    | String | page/post title                |
| snippet  | String | relevant excerpt               |
| date     | String | ISO date or null               |
| work-url | String | the work being mentioned       |

### Frappe Endpoint

@frappe.whitelist()
def search_mentions(work_url: str) -> list[dict]:
    """Calls Exa, returns mentions of the given work."""
    pass

### Frappe DocType: Mention

Fields mirror the Mention object shape above. Used for display/querying on the Frappe side.
