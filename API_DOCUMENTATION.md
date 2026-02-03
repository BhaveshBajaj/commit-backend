# Commitment System API Documentation

## Overview

The Commitment System records explicit mutual agreements between parties with immutability guarantees. Once all parties approve, a commitment becomes **permanently locked** and cannot be modified.

### Base URL
```
http://localhost:8080
```

### Authentication
All endpoints require the `X-USER-ID` header containing the current user's ID.

```http
X-USER-ID: 1
```

---

## Core Concepts

### Spaces
A **Space** is a contextual container (e.g., "Work Team", "Friends", "Project Alpha"). Users must be **approved members** of a space to create or view commitments within it.

### Space Membership

Users join spaces via invitations. Membership has three states:

```
┌─────────┐     accept      ┌──────────┐
│ PENDING │ ──────────────▶ │ APPROVED │
└─────────┘                 └──────────┘
     │
     │ reject / expire (1 week)
     ▼
┌──────────┐
│ REJECTED │ ◀── can be re-invited
└──────────┘
```

| Status | Description |
|--------|-------------|
| `PENDING` | Invite sent, awaiting response |
| `APPROVED` | Full member, can create/view commitments |
| `REJECTED` | Declined or expired (can be re-invited) |

### Commitments
A **Commitment** is an agreement statement that requires explicit approval from all designated approvers.

### Commitment Lifecycle

```
┌─────────┐     send for      ┌─────────┐     all approve     ┌─────────┐
│  DRAFT  │ ───────────────▶  │ REVIEW  │ ─────────────────▶  │ LOCKED  │
└─────────┘     review        └─────────┘                     └─────────┘
     ▲                              │
     │         rejection            │
     └──────────────────────────────┘
```

| Status | Description | Editable | Can Approve/Reject |
|--------|-------------|----------|-------------------|
| `DRAFT` | Work in progress | ✅ Yes | ❌ No |
| `REVIEW` | Awaiting approvals | ❌ No | ✅ Yes |
| `LOCKED` | Immutable forever | ❌ No | ❌ No |

### Approver Status

| Status | Description |
|--------|-------------|
| `PENDING` | Has not acted yet |
| `APPROVED` | Explicitly approved |
| `REJECTED` | Rejected (resets commitment to DRAFT) |

---

## API Endpoints

### 1. Spaces

#### 1.1 Create Space
Creates a new space and adds the creator as a member.

```http
POST /spaces
```

**Headers**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| X-USER-ID | Long | Yes | Current user's ID |
| Content-Type | String | Yes | `application/json` |

**Request Body**
```json
{
  "name": "Backend Team",
  "description": "API development team"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Yes | Space name |
| description | String | No | Space description |

**Response** `201 Created`
```json
{
  "id": 1,
  "name": "Backend Team",
  "description": "API development team",
  "createdBy": 1,
  "createdAt": "2026-02-02T16:30:00Z"
}
```

**Errors**
| Status | Error | Cause |
|--------|-------|-------|
| 400 | `name: must not be blank` | Missing name |
| 404 | `User not found` | Invalid X-USER-ID |

---

#### 1.2 Get User's Spaces
Returns all spaces the current user is a member of.

```http
GET /spaces
```

**Headers**
| Name | Type | Required |
|------|------|----------|
| X-USER-ID | Long | Yes |

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Backend Team",
    "description": "API development team",
    "createdBy": 1,
    "createdAt": "2026-02-02T16:30:00Z"
  },
  {
    "id": 2,
    "name": "Project Alpha",
    "description": null,
    "createdBy": 3,
    "createdAt": "2026-02-01T10:00:00Z"
  }
]
```

---

#### 1.3 Get Space Details
Returns details of a specific space.

```http
GET /spaces/{spaceId}
```

**Path Parameters**
| Name | Type | Description |
|------|------|-------------|
| spaceId | Long | Space ID |

**Response** `200 OK`
```json
{
  "id": 1,
  "name": "Backend Team",
  "description": "API development team",
  "createdBy": 1,
  "createdAt": "2026-02-02T16:30:00Z"
}
```

**Errors**
| Status | Error |
|--------|-------|
| 404 | `Space not found` |

---

#### 1.4 Invite User to Space
Invites a user to join the space by email. Only **approved members** can send invites.

```http
POST /spaces/{spaceId}/invite
```

**Path Parameters**
| Name | Type | Description |
|------|------|-------------|
| spaceId | Long | Space ID |

**Headers**
| Name | Type | Required |
|------|------|----------|
| X-USER-ID | Long | Yes |
| Content-Type | String | Yes |

**Request Body**
```json
{
  "email": "bob@example.com"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| email | String | Yes | Email of user to invite (must be valid email format) |

**Response** `201 Created`
Empty body

**Errors**
| Status | Error | Cause |
|--------|-------|-------|
| 400 | `email: must not be blank` | Missing email |
| 400 | `User is already a member` | User has APPROVED status |
| 400 | `Invite already pending` | User has PENDING status |
| 403 | `Only approved members can invite` | Inviter not approved member |
| 404 | `Space not found` | Invalid spaceId |
| 404 | `User not found. Ask them to join the platform first.` | Email not registered |

**Notes**
- If user previously rejected, they can be re-invited
- Creator is auto-approved when space is created

---

### 2. Invites

#### 2.1 Get Pending Invites
Returns all pending space invites for the current user.

```http
GET /invites
```

**Headers**
| Name | Type | Required |
|------|------|----------|
| X-USER-ID | Long | Yes |

**Response** `200 OK`
```json
[
  {
    "id": 5,
    "spaceId": 1,
    "spaceName": "Backend Team",
    "status": "PENDING",
    "invitedAt": "2026-02-03T10:00:00Z"
  },
  {
    "id": 8,
    "spaceId": 3,
    "spaceName": "Project Alpha",
    "status": "PENDING",
    "invitedAt": "2026-02-02T15:30:00Z"
  }
]
```

---

#### 2.2 Accept Invite
Accepts a pending invite, making the user an approved member of the space.

```http
POST /invites/{id}/accept
```

**Path Parameters**
| Name | Type | Description |
|------|------|-------------|
| id | Long | Invite ID (from GET /invites response) |

**Headers**
| Name | Type | Required |
|------|------|----------|
| X-USER-ID | Long | Yes |

**Response** `200 OK`
Empty body

**Errors**
| Status | Error | Cause |
|--------|-------|-------|
| 400 | `Invite is not pending` | Already accepted/rejected |
| 404 | `Invite not found` | Invalid ID or not user's invite |

---

#### 2.3 Reject Invite
Rejects a pending invite.

```http
POST /invites/{id}/reject
```

**Path Parameters**
| Name | Type | Description |
|------|------|-------------|
| id | Long | Invite ID (from GET /invites response) |

**Headers**
| Name | Type | Required |
|------|------|----------|
| X-USER-ID | Long | Yes |

**Response** `200 OK`
Empty body

**Errors**
| Status | Error | Cause |
|--------|-------|-------|
| 400 | `Invite is not pending` | Already accepted/rejected |
| 404 | `Invite not found` | Invalid ID or not user's invite |

---

### 3. Commitments

#### 3.1 Create Commitment (Draft)
Creates a new commitment in DRAFT status.

```http
POST /spaces/{spaceId}/commitments
```

**Path Parameters**
| Name | Type | Description |
|------|------|-------------|
| spaceId | Long | Space ID |

**Headers**
| Name | Type | Required |
|------|------|----------|
| X-USER-ID | Long | Yes |
| Content-Type | String | Yes |

**Request Body**
```json
{
  "title": "API Contract v1",
  "description": "Backend will expose /v1/orders endpoint with pagination",
  "deadline": "2026-02-10T00:00:00Z",
  "approverIds": [2, 3]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | String | Yes | Commitment title |
| description | String | No | Detailed description |
| deadline | ISO 8601 DateTime | No | Optional deadline |
| approverIds | Array[Long] | Yes | User IDs who must approve (creator auto-added) |

**Response** `201 Created`
```json
{
  "id": 1,
  "spaceId": 1,
  "title": "API Contract v1",
  "description": "Backend will expose /v1/orders endpoint with pagination",
  "status": "DRAFT",
  "createdBy": 1,
  "createdAt": "2026-02-02T16:45:00Z",
  "deadline": "2026-02-10T00:00:00Z",
  "approvers": [
    { "userId": 1, "status": "PENDING", "actedAt": null },
    { "userId": 2, "status": "PENDING", "actedAt": null },
    { "userId": 3, "status": "PENDING", "actedAt": null }
  ]
}
```

**Errors**
| Status | Error | Cause |
|--------|-------|-------|
| 400 | `title: must not be blank` | Missing title |
| 400 | `approverIds: must not be empty` | No approvers specified |
| 400 | `Approver X not member of space` | Approver not in space |
| 403 | `User not member of space` | Creator not in space |
| 404 | `One or more approvers not found` | Invalid approver ID |

---

#### 3.2 Get Commitment Details
Returns commitment with all approver statuses.

```http
GET /commitments/{id}
```

**Path Parameters**
| Name | Type | Description |
|------|------|-------------|
| id | Long | Commitment ID |

**Headers**
| Name | Type | Required |
|------|------|----------|
| X-USER-ID | Long | Yes |

**Response** `200 OK`
```json
{
  "id": 1,
  "spaceId": 1,
  "title": "API Contract v1",
  "description": "Backend will expose /v1/orders endpoint with pagination",
  "status": "REVIEW",
  "createdBy": 1,
  "createdAt": "2026-02-02T16:45:00Z",
  "deadline": "2026-02-10T00:00:00Z",
  "approvers": [
    { "userId": 1, "status": "APPROVED", "actedAt": "2026-02-02T17:00:00Z" },
    { "userId": 2, "status": "PENDING", "actedAt": null },
    { "userId": 3, "status": "PENDING", "actedAt": null }
  ]
}
```

**Errors**
| Status | Error |
|--------|-------|
| 403 | `User not member of space` |
| 404 | `Commitment not found` |

---

#### 3.3 List Space Commitments
Returns all commitments in a space.

```http
GET /spaces/{spaceId}/commitments
```

**Path Parameters**
| Name | Type | Description |
|------|------|-------------|
| spaceId | Long | Space ID |

**Headers**
| Name | Type | Required |
|------|------|----------|
| X-USER-ID | Long | Yes |

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "spaceId": 1,
    "title": "API Contract v1",
    "status": "LOCKED",
    "createdBy": 1,
    "createdAt": "2026-02-02T16:45:00Z",
    "deadline": "2026-02-10T00:00:00Z",
    "approvers": [...]
  },
  {
    "id": 2,
    "spaceId": 1,
    "title": "Database Schema Agreement",
    "status": "DRAFT",
    "createdBy": 2,
    "createdAt": "2026-02-02T18:00:00Z",
    "deadline": null,
    "approvers": [...]
  }
]
```

---

#### 3.4 Update Commitment (Draft Only)
Updates a commitment. **Only allowed when status is DRAFT.**

```http
PUT /commitments/{id}
```

**Path Parameters**
| Name | Type | Description |
|------|------|-------------|
| id | Long | Commitment ID |

**Request Body**
```json
{
  "title": "API Contract v1.1",
  "description": "Updated: Now includes cursor pagination",
  "deadline": "2026-02-15T00:00:00Z"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | String | No | New title (if provided) |
| description | String | No | New description (if provided) |
| deadline | ISO 8601 DateTime | No | New deadline (if provided) |

**Response** `200 OK`
Returns updated commitment object.

**Errors**
| Status | Error | Cause |
|--------|-------|-------|
| 400 | `Can only edit DRAFT commitments` | Status is REVIEW or LOCKED |
| 404 | `Commitment not found` | Invalid ID |

---

#### 3.5 Send for Review
Moves commitment from DRAFT to REVIEW. Approvers can now approve/reject.

```http
POST /commitments/{id}/review
```

**Path Parameters**
| Name | Type | Description |
|------|------|-------------|
| id | Long | Commitment ID |

**Headers**
| Name | Type | Required |
|------|------|----------|
| X-USER-ID | Long | Yes |

**Response** `200 OK`
```json
{
  "id": 1,
  "status": "REVIEW",
  ...
}
```

**Errors**
| Status | Error | Cause |
|--------|-------|-------|
| 400 | `Can only send DRAFT for review` | Already in REVIEW or LOCKED |

---

#### 3.6 Approve Commitment
Records approval from the current user. **If all approvers approve, commitment becomes LOCKED.**

```http
POST /commitments/{id}/approve
```

**Path Parameters**
| Name | Type | Description |
|------|------|-------------|
| id | Long | Commitment ID |

**Headers**
| Name | Type | Required |
|------|------|----------|
| X-USER-ID | Long | Yes |

**Response** `200 OK`
```json
{
  "id": 1,
  "status": "LOCKED",
  "approvers": [
    { "userId": 1, "status": "APPROVED", "actedAt": "2026-02-02T17:00:00Z" },
    { "userId": 2, "status": "APPROVED", "actedAt": "2026-02-02T17:05:00Z" }
  ]
}
```

**Errors**
| Status | Error | Cause |
|--------|-------|-------|
| 400 | `Can only approve commitments in REVIEW` | Status is DRAFT or LOCKED |
| 400 | `Already acted on this commitment` | User already approved/rejected |
| 403 | `User is not an approver` | Not designated as approver |

---

#### 3.7 Reject Commitment
Rejects the commitment. **Resets status to DRAFT and all approvers to PENDING.**

```http
POST /commitments/{id}/reject
```

**Path Parameters**
| Name | Type | Description |
|------|------|-------------|
| id | Long | Commitment ID |

**Headers**
| Name | Type | Required |
|------|------|----------|
| X-USER-ID | Long | Yes |

**Response** `200 OK`
```json
{
  "id": 1,
  "status": "DRAFT",
  "approvers": [
    { "userId": 1, "status": "PENDING", "actedAt": null },
    { "userId": 2, "status": "PENDING", "actedAt": null }
  ]
}
```

**Errors**
| Status | Error | Cause |
|--------|-------|-------|
| 400 | `Can only reject commitments in REVIEW` | Status is DRAFT or LOCKED |
| 400 | `Already acted on this commitment` | User already approved/rejected |
| 403 | `User is not an approver` | Not designated as approver |

---

## Complete User Journey

### Scenario: Two developers agree on an API contract

#### Step 1: Setup
Assume we have two users in the database:
- User 1: Alice (Backend Developer) - email: alice@example.com
- User 2: Bob (Frontend Developer) - email: bob@example.com

#### Step 2: Alice creates a space

```http
POST /spaces
X-USER-ID: 1
Content-Type: application/json

{
  "name": "Project Phoenix",
  "description": "Mobile app backend"
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Project Phoenix",
  "createdBy": 1,
  ...
}
```

*Alice is auto-approved as a member*

#### Step 3: Alice invites Bob to the space

```http
POST /spaces/1/invite
X-USER-ID: 1
Content-Type: application/json

{
  "email": "bob@example.com"
}
```

**Response:** `201 Created` (empty body)

#### Step 4: Bob checks his pending invites

```http
GET /invites
X-USER-ID: 2
```

**Response:**
```json
[
  {
    "id": 5,
    "spaceId": 1,
    "spaceName": "Project Phoenix",
    "status": "PENDING",
    "invitedAt": "2026-02-03T10:00:00Z"
  }
]
```

#### Step 5: Bob accepts the invite

```http
POST /invites/5/accept
X-USER-ID: 2
```

**Response:** `200 OK` (empty body)

#### Step 6: Bob verifies he's in the space

```http
GET /spaces
X-USER-ID: 2
```

**Response:**
```json
[
  { "id": 1, "name": "Project Phoenix", ... }
]
```

#### Step 7: Alice creates a commitment draft

```http
POST /spaces/1/commitments
X-USER-ID: 1
Content-Type: application/json

{
  "title": "Orders API Contract",
  "description": "Backend will expose GET /v1/orders returning paginated results with max 100 items per page",
  "deadline": "2026-02-10T00:00:00Z",
  "approverIds": [2]
}
```

**Response:**
```json
{
  "id": 1,
  "status": "DRAFT",
  "approvers": [
    { "userId": 1, "status": "PENDING", "actedAt": null },
    { "userId": 2, "status": "PENDING", "actedAt": null }
  ]
}
```

#### Step 8: Alice edits the draft

```http
PUT /commitments/1
X-USER-ID: 1
Content-Type: application/json

{
  "description": "Backend will expose GET /v1/orders with cursor-based pagination, max 100 items, sorted by created_at DESC"
}
```

#### Step 9: Alice sends for review

```http
POST /commitments/1/review
X-USER-ID: 1
```

**Response:**
```json
{
  "id": 1,
  "status": "REVIEW",
  ...
}
```

#### Step 10: Bob reviews and rejects (needs change)

```http
POST /commitments/1/reject
X-USER-ID: 2
```

**Response:**
```json
{
  "id": 1,
  "status": "DRAFT",
  "approvers": [
    { "userId": 1, "status": "PENDING", "actedAt": null },
    { "userId": 2, "status": "PENDING", "actedAt": null }
  ]
}
```

*All approvers reset to PENDING*

#### Step 11: Alice edits and resubmits

```http
PUT /commitments/1
X-USER-ID: 1
Content-Type: application/json

{
  "description": "Backend will expose GET /v1/orders with offset pagination (page, limit params), max 50 items default"
}
```

```http
POST /commitments/1/review
X-USER-ID: 1
```

#### Step 12: Alice approves

```http
POST /commitments/1/approve
X-USER-ID: 1
```

**Response:**
```json
{
  "id": 1,
  "status": "REVIEW",
  "approvers": [
    { "userId": 1, "status": "APPROVED", "actedAt": "2026-02-02T17:00:00Z" },
    { "userId": 2, "status": "PENDING", "actedAt": null }
  ]
}
```

#### Step 13: Bob approves → LOCKED

```http
POST /commitments/1/approve
X-USER-ID: 2
```

**Response:**
```json
{
  "id": 1,
  "status": "LOCKED",
  "approvers": [
    { "userId": 1, "status": "APPROVED", "actedAt": "2026-02-02T17:00:00Z" },
    { "userId": 2, "status": "APPROVED", "actedAt": "2026-02-02T17:05:00Z" }
  ]
}
```

🔒 **The commitment is now permanently immutable!**

#### Step 14: Verify immutability (will fail)

```http
PUT /commitments/1
X-USER-ID: 1
Content-Type: application/json

{
  "title": "Changed my mind"
}
```

**Response:** `400 Bad Request`
```json
{
  "error": "Can only edit DRAFT commitments"
}
```

---

## Frontend Integration Tips

### 1. State-based UI rendering

```javascript
// Show/hide buttons based on commitment status
const renderActions = (commitment, currentUserId) => {
  const userApprover = commitment.approvers.find(a => a.userId === currentUserId);
  
  switch (commitment.status) {
    case 'DRAFT':
      return (
        <>
          <EditButton />
          <SendForReviewButton />
        </>
      );
    
    case 'REVIEW':
      if (userApprover && userApprover.status === 'PENDING') {
        return (
          <>
            <ApproveButton />
            <RejectButton />
          </>
        );
      }
      return <WaitingForOthersLabel />;
    
    case 'LOCKED':
      return <LockedBadge />;
  }
};
```

### 2. Approval progress indicator

```javascript
const ApprovalProgress = ({ approvers }) => {
  const approved = approvers.filter(a => a.status === 'APPROVED').length;
  const total = approvers.length;
  
  return (
    <ProgressBar 
      value={approved} 
      max={total} 
      label={`${approved}/${total} approved`}
    />
  );
};
```

### 3. Error handling

```javascript
const handleApiError = (error) => {
  const message = error.response?.data?.error || 'Something went wrong';
  
  switch (error.response?.status) {
    case 400:
      showToast({ type: 'warning', message });
      break;
    case 403:
      showToast({ type: 'error', message: 'Access denied' });
      break;
    case 404:
      showToast({ type: 'error', message: 'Not found' });
      break;
    default:
      showToast({ type: 'error', message });
  }
};
```

### 4. Polling for updates (simple approach)

```javascript
// Poll for commitment updates when in REVIEW status
useEffect(() => {
  if (commitment.status !== 'REVIEW') return;
  
  const interval = setInterval(async () => {
    const updated = await fetchCommitment(commitment.id);
    if (updated.status !== commitment.status) {
      setCommitment(updated);
    }
  }, 5000);
  
  return () => clearInterval(interval);
}, [commitment.id, commitment.status]);
```

---

## Response Type Definitions (TypeScript)

```typescript
interface SpaceResponse {
  id: number;
  name: string;
  description: string | null;
  createdBy: number;
  createdAt: string; // ISO 8601
}

interface InviteResponse {
  id: number;
  spaceId: number;
  spaceName: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  invitedAt: string; // ISO 8601
}

interface ApproverResponse {
  userId: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  actedAt: string | null; // ISO 8601
}

interface CommitmentResponse {
  id: number;
  spaceId: number;
  title: string;
  description: string | null;
  status: 'DRAFT' | 'REVIEW' | 'LOCKED';
  createdBy: number;
  createdAt: string; // ISO 8601
  deadline: string | null; // ISO 8601
  approvers: ApproverResponse[];
}

interface ErrorResponse {
  error: string;
}
```

---

## HTTP Status Codes Summary

| Code | Meaning | When |
|------|---------|------|
| 200 | OK | Successful GET, PUT, or action |
| 201 | Created | Successful POST creating resource |
| 400 | Bad Request | Validation error or invalid state transition |
| 403 | Forbidden | Not authorized (not member, not approver) |
| 404 | Not Found | Resource doesn't exist |
| 500 | Server Error | Unexpected error |
