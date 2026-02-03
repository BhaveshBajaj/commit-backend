# Missing API Requirements for V1

> **Document Version**: 1.0  
> **Last Updated**: 3 February 2026  
> **Status**: Pending Backend Implementation

This document outlines the API endpoints required for the frontend that are **not yet documented** in `API_DOCUMENTATION.md`.

---

## 1. User Endpoints

### 1.1 Get User by ID

Fetch user details to display names/avatars for approvers and space members.

```
GET /users/{userId}
```

**Headers**
| Header | Required | Description |
|--------|----------|-------------|
| `X-USER-ID` | Yes | Current authenticated user ID |

**Path Parameters**
| Parameter | Type | Description |
|-----------|------|-------------|
| `userId` | number | ID of the user to fetch |

**Success Response** `200 OK`
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "avatarUrl": "https://..." // optional, can be null
}
```

**Error Responses**
| Status | Description |
|--------|-------------|
| `404` | User not found |

---

### 1.2 Get Current User

Fetch the currently authenticated user's profile.

```
GET /users/me
```

**Headers**
| Header | Required | Description |
|--------|----------|-------------|
| `X-USER-ID` | Yes | Current authenticated user ID |

**Success Response** `200 OK`
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "avatarUrl": "https://..." // optional, can be null
}
```

---

### 1.3 Search Users (for Approver Selection)

Search users within a space to add as approvers.

```
GET /spaces/{spaceId}/members/search?q={query}
```

**Headers**
| Header | Required | Description |
|--------|----------|-------------|
| `X-USER-ID` | Yes | Current authenticated user ID |

**Query Parameters**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `q` | string | Yes | Search query (name or email) |

**Success Response** `200 OK`
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "avatarUrl": null
  },
  {
    "id": 2,
    "name": "Jane Smith",
    "email": "jane@example.com",
    "avatarUrl": "https://..."
  }
]
```

---

## 2. Space Endpoints

### 2.1 Leave Space

Allow a user to leave a space they are a member of.

```
POST /spaces/{spaceId}/leave
```

**Headers**
| Header | Required | Description |
|--------|----------|-------------|
| `X-USER-ID` | Yes | Current authenticated user ID |

**Path Parameters**
| Parameter | Type | Description |
|-----------|------|-------------|
| `spaceId` | number | ID of the space to leave |

**Success Response** `200 OK`
```json
{
  "message": "Successfully left the space"
}
```

**Error Responses**
| Status | Description |
|--------|-------------|
| `400` | Cannot leave space (e.g., creator cannot leave, or has pending commitments) |
| `404` | Space not found or user not a member |

**Business Rules**
- Space creator **cannot** leave their own space
- User with commitments in REVIEW status may have restrictions (TBD)

---

### 2.2 Get Space Members

List all members of a space with their details.

```
GET /spaces/{spaceId}/members
```

**Headers**
| Header | Required | Description |
|--------|----------|-------------|
| `X-USER-ID` | Yes | Current authenticated user ID |

**Success Response** `200 OK`
```json
[
  {
    "userId": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "avatarUrl": null,
    "role": "creator",
    "joinedAt": "2026-01-15T10:30:00Z"
  },
  {
    "userId": 2,
    "name": "Jane Smith",
    "email": "jane@example.com",
    "avatarUrl": "https://...",
    "role": "member",
    "joinedAt": "2026-01-20T14:00:00Z"
  }
]
```

---

## 3. Commitment Endpoints

### 3.1 Get Commitment History (Audit Trail)

Fetch the complete audit trail for a commitment.

```
GET /commitments/{commitmentId}/history
```

**Headers**
| Header | Required | Description |
|--------|----------|-------------|
| `X-USER-ID` | Yes | Current authenticated user ID |

**Success Response** `200 OK`
```json
[
  {
    "id": 1,
    "action": "CREATED",
    "performedBy": {
      "id": 1,
      "name": "John Doe"
    },
    "timestamp": "2026-01-15T10:30:00Z",
    "details": null
  },
  {
    "id": 2,
    "action": "SENT_FOR_REVIEW",
    "performedBy": {
      "id": 1,
      "name": "John Doe"
    },
    "timestamp": "2026-01-16T09:00:00Z",
    "details": null
  },
  {
    "id": 3,
    "action": "APPROVED",
    "performedBy": {
      "id": 2,
      "name": "Jane Smith"
    },
    "timestamp": "2026-01-16T14:30:00Z",
    "details": null
  },
  {
    "id": 4,
    "action": "REJECTED",
    "performedBy": {
      "id": 3,
      "name": "Bob Wilson"
    },
    "timestamp": "2026-01-17T11:00:00Z",
    "details": {
      "reason": "Terms need clarification" // optional
    }
  }
]
```

**Action Types**
| Action | Description |
|--------|-------------|
| `CREATED` | Commitment was created |
| `UPDATED` | Commitment was edited (DRAFT only) |
| `SENT_FOR_REVIEW` | Moved from DRAFT to REVIEW |
| `APPROVED` | An approver approved |
| `REJECTED` | An approver rejected (resets to DRAFT) |
| `LOCKED` | All approvers approved, commitment locked |

---

## 4. TypeScript Interfaces

Add these to the frontend type definitions:

```typescript
interface UserResponse {
  id: number;
  name: string;
  email: string;
  avatarUrl: string | null;
}

interface SpaceMemberResponse {
  userId: number;
  name: string;
  email: string;
  avatarUrl: string | null;
  role: 'creator' | 'member';
  joinedAt: string; // ISO 8601
}

interface CommitmentHistoryEntry {
  id: number;
  action: 'CREATED' | 'UPDATED' | 'SENT_FOR_REVIEW' | 'APPROVED' | 'REJECTED' | 'LOCKED';
  performedBy: {
    id: number;
    name: string;
  };
  timestamp: string; // ISO 8601
  details: Record<string, unknown> | null;
}
```

---

## Summary

| Endpoint | Method | Priority | Purpose |
|----------|--------|----------|---------|
| `/users/{userId}` | GET | **High** | Display user names |
| `/users/me` | GET | **High** | Current user profile |
| `/spaces/{spaceId}/members` | GET | **High** | List space members |
| `/spaces/{spaceId}/members/search` | GET | **Medium** | Search for approvers |
| `/spaces/{spaceId}/leave` | POST | **Medium** | Leave a space |
| `/commitments/{id}/history` | GET | **Medium** | Audit trail |

---

## Notes for Backend Team

1. All endpoints should follow existing patterns (error format, headers, etc.)
2. User avatars can use [DiceBear](https://dicebear.com/) or [UI Avatars](https://ui-avatars.com/) as fallback
3. Consider pagination for `/spaces/{spaceId}/members` if spaces can have many members
4. History entries should be immutable - append-only log
