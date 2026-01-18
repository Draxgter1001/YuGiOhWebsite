# 🎴 DuelDiskScan.gg - Yu-Gi-Oh! Card Scanner & Deck Builder

<div align="center">

![Yu-Gi-Oh Scanner](https://img.shields.io/badge/Yu--Gi--Oh!-Card%20Scanner-gold?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiPjxjaXJjbGUgY3g9IjEyIiBjeT0iMTIiIHI9IjEwIi8+PGNpcmNsZSBjeD0iMTIiIGN5PSIxMiIgcj0iMyIvPjwvc3ZnPg==)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)

**A full-stack web application for scanning, identifying, and managing Yu-Gi-Oh! trading cards with real-time market prices and deck building capabilities.**

[Live Demo](#https://yu-gi-oh-website-plum.vercel.app/) • [Features](#-features) • [Tech Stack](#-tech-stack) • [Getting Started](#-getting-started) • [API Documentation](#-api-documentation)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Security Implementation](#-security-implementation)
- [Deployment](#-deployment)
- [Future Enhancements](#-future-enhancements)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Overview

**DuelDiskScan.gg** is a comprehensive Yu-Gi-Oh! card management platform that combines **OCR technology**, **real-time market data**, and **deck building tools** into a seamless user experience. Whether you're a collector wanting to catalog your cards or a competitive player building tournament-ready decks, this application provides all the tools you need.

### The Problem It Solves

- **Card Identification**: Quickly identify cards from photos without manual searching
- **Price Tracking**: Compare prices across multiple marketplaces (TCGPlayer, Cardmarket, CoolStuffInc)
- **Deck Management**: Build, validate, and manage decks according to official Yu-Gi-Oh! rules
- **Collection Organization**: Keep track of your card collection digitally

---

## ✨ Features

### 🔍 Card Scanner & Search
- **AI-Powered OCR**: Upload card images and automatically extract card names using external OCR service
- **Smart Autocomplete Search**: Real-time search suggestions as you type
- **Fuzzy Matching**: Handles typos and partial names gracefully
- **Card Database Caching**: Frequently accessed cards are cached locally for faster retrieval

### 💰 Market Prices
- **Multi-Marketplace Support**: View prices from TCGPlayer, Cardmarket, and CoolStuffInc
- **Direct Purchase Links**: Click-through links to buy cards from your preferred marketplace
- **Deck Value Calculator**: Automatically calculate total deck value across all marketplaces

### 📚 Deck Builder
- **Full Deck Management**: Create, edit, and delete multiple decks
- **Yu-Gi-Oh! Rules Validation**: Automatic validation against official deck building rules
  - Main Deck: 40-60 cards
  - Extra Deck: 0-15 cards (Fusion, Synchro, XYZ, Link monsters)
  - Side Deck: 0-15 cards
  - Maximum 3 copies of any card
- **Auto-Sort Cards**: Extra deck cards automatically sorted to correct deck section
- **Public/Private Decks**: Share your decks with the community or keep them private

### 🔐 User Authentication
- **Secure JWT Authentication**: Access and refresh token system
- **Password Recovery**: Email-based password reset functionality
- **Username Recovery**: Forgot username? Get it sent to your email
- **Rate Limiting**: Protection against brute-force attacks

### 📱 User Experience
- **Responsive Design**: Works seamlessly on desktop, tablet, and mobile
- **Dark Theme**: Eye-friendly dark mode optimized for card viewing
- **Drag & Drop Upload**: Easy card image uploading
- **Recent Scans History**: Quick access to recently scanned cards

---

## 🛠 Tech Stack

### Backend
| Technology | Purpose |
|------------|---------|
| **Java 21** | Primary backend language with latest LTS features |
| **Spring Boot 3.2.4** | Application framework with auto-configuration |
| **Spring Security** | Authentication and authorization |
| **Spring Data JPA** | Database ORM and repository pattern |
| **PostgreSQL** | Primary relational database |
| **Caffeine Cache** | In-memory caching for performance |
| **JWT (jjwt 0.12.5)** | Stateless authentication tokens |
| **JavaMail** | Email service for password recovery |

### Frontend
| Technology | Purpose |
|------------|---------|
| **React 18** | UI component library |
| **React Router v6** | Client-side routing |
| **Lucide React** | Icon library |
| **CSS3 Variables** | Theming and styling |

### Infrastructure
| Technology | Purpose |
|------------|---------|
| **Docker** | Containerization |
| **Heroku** | Cloud deployment platform |
| **HuggingFace Spaces** | External OCR service hosting |

### External APIs
| API | Purpose |
|-----|---------|
| **YGOPRODeck API** | Card database and information |
| **Custom OCR Service** | Card name extraction from images |

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT (React SPA)                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Scanner   │  │   Search    │  │Deck Builder │  │   Authentication    │ │
│  │  Component  │  │  Component  │  │  Component  │  │     Components      │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
└─────────┼────────────────┼────────────────┼────────────────────┼────────────┘
          │                │                │                    │
          ▼                ▼                ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           API SERVICE LAYER                                  │
│                     (Axios/Fetch with JWT interceptor)                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        SPRING BOOT BACKEND                                   │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                     SECURITY FILTER CHAIN                             │   │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────────────┐  │   │
│  │  │ Rate Limiting  │─▶│ JWT Validation │─▶│ Security Headers Filter│  │   │
│  │  └────────────────┘  └────────────────┘  └────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        REST CONTROLLERS                               │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────┐  │   │
│  │  │AuthController│  │CardController│  │DeckController│  │  Image   │  │   │
│  │  │  /api/auth   │  │  /api/cards  │  │  /api/decks  │  │Controller│  │   │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └────┬─────┘  │   │
│  └─────────┼─────────────────┼─────────────────┼───────────────┼────────┘   │
│            │                 │                 │               │            │
│  ┌─────────▼─────────────────▼─────────────────▼───────────────▼────────┐   │
│  │                        SERVICE LAYER                                  │   │
│  │  ┌───────────┐  ┌────────────┐  ┌───────────┐  ┌──────────────────┐  │   │
│  │  │UserService│  │YugiohAPI   │  │DeckService│  │DatabaseImage     │  │   │
│  │  │JwtService │  │Service     │  │           │  │Service           │  │   │
│  │  │EmailSvc   │  │CardOCRSvc  │  │           │  │                  │  │   │
│  │  └───────────┘  └─────┬──────┘  └───────────┘  └──────────────────┘  │   │
│  └───────────────────────┼──────────────────────────────────────────────┘   │
│                          │                                                   │
│  ┌───────────────────────▼──────────────────────────────────────────────┐   │
│  │                     REPOSITORY LAYER (JPA)                            │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │   │
│  │  │  User    │ │  Card    │ │UserDeck  │ │ DeckCard │ │ CardImage  │  │   │
│  │  │Repository│ │Repository│ │Repository│ │Repository│ │ Repository │  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
          │                          │                           │
          ▼                          ▼                           ▼
┌──────────────────┐    ┌──────────────────────┐    ┌─────────────────────────┐
│   PostgreSQL     │    │  YGOPRODeck API      │    │   OCR Service           │
│   Database       │    │  (Card Information)  │    │   (HuggingFace)         │
└──────────────────┘    └──────────────────────┘    └─────────────────────────┘
```

### Data Flow

1. **Card Scanning Flow**:
   ```
   User uploads image → Backend → OCR Service → Extract card name → 
   YGOPRODeck API → Card details + prices → Store in DB → Return to frontend
   ```

2. **Authentication Flow**:
   ```
   Login request → Validate credentials → Generate JWT pair → 
   Store refresh token → Return tokens → Frontend stores in localStorage
   ```

3. **Deck Building Flow**:
   ```
   Add card → Validate against rules → Check copy limits → 
   Determine deck section (Main/Extra/Side) → Update database → 
   Recalculate prices → Return updated deck
   ```

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher
- **Node.js 18** or higher
- **PostgreSQL 15** or higher
- **Maven 3.9+**
- **Docker** (optional, for containerized deployment)

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/yugioh-scanner.git
   cd yugioh-scanner
   ```

2. **Configure the database**
   ```bash
   # Create PostgreSQL database
   createdb yugioh_scanner
   ```

3. **Set up environment variables** (see [Environment Variables](#-environment-variables))

4. **Build and run**
   ```bash
   # Build the project
   mvn clean package -DskipTests
   
   # Run the application
   java -jar target/yugioh-scanner-0.0.1-SNAPSHOT.jar
   ```

### Frontend Setup

1. **Navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure environment**
   ```bash
   # Create .env file
   echo "REACT_APP_API_URL=http://localhost:8080/api" > .env
   ```

4. **Start development server**
   ```bash
   npm start
   ```

### Docker Setup

```bash
# Build the Docker image
docker build -t yugioh-scanner .

# Run with environment variables
docker run -p 8080:8080 \
  -e DATABASE_URL=postgres://user:pass@host:5432/dbname \
  -e JWT_SECRET=your-secret-key \
  yugioh-scanner
```

---

## 🔧 Environment Variables

### Backend (application.properties or environment)

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `DATABASE_URL` | PostgreSQL connection URL (Heroku format) | Yes | - |
| `SPRING_DATASOURCE_URL` | JDBC connection URL | Yes* | - |
| `SPRING_DATASOURCE_USERNAME` | Database username | Yes* | - |
| `SPRING_DATASOURCE_PASSWORD` | Database password | Yes* | - |
| `JWT_SECRET` | Secret key for JWT signing (min 256 bits) | Yes | - |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token TTL in ms | No | 900000 (15min) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token TTL in ms | No | 604800000 (7d) |
| `APP_PYTHON_OCR_URL` | OCR service endpoint | Yes | - |
| `APP_BACKEND_URL` | Backend URL for image serving | Yes | http://localhost:8080 |
| `APP_FRONTEND_URL` | Frontend URL for email links | Yes | http://localhost:3000 |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | Yes | http://localhost:3000 |
| `SPRING_MAIL_HOST` | SMTP server host | Yes | - |
| `SPRING_MAIL_PORT` | SMTP server port | No | 587 |
| `SPRING_MAIL_USERNAME` | SMTP username | Yes | - |
| `SPRING_MAIL_PASSWORD` | SMTP password | Yes | - |

*Required if `DATABASE_URL` is not set (Heroku auto-converts)

### Frontend

| Variable | Description | Required |
|----------|-------------|----------|
| `REACT_APP_API_URL` | Backend API base URL | Yes |

---

## 📖 API Documentation

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login with credentials | No |
| POST | `/api/auth/refresh` | Refresh access token | No |
| GET | `/api/auth/me` | Get current user info | Yes |
| GET | `/api/auth/validate` | Validate token | Yes |
| POST | `/api/auth/forgot-password` | Request password reset | No |
| GET | `/api/auth/verify-reset-token` | Verify reset token | No |
| POST | `/api/auth/reset-password` | Reset password | No |
| POST | `/api/auth/forgot-username` | Request username reminder | No |

### Card Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/cards/upload` | Upload card image for OCR | No |
| GET | `/api/cards/search?name={name}` | Search card by exact name | No |
| GET | `/api/cards/autocomplete?q={query}` | Autocomplete search | No |

### Deck Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/decks` | Get user's decks | Yes |
| POST | `/api/decks` | Create new deck | Yes |
| GET | `/api/decks/{id}` | Get deck details | Yes |
| PUT | `/api/decks/{id}` | Update deck info | Yes |
| DELETE | `/api/decks/{id}` | Delete deck | Yes |
| POST | `/api/decks/{id}/cards` | Add card to deck | Yes |
| DELETE | `/api/decks/{id}/cards/{cardId}` | Remove card from deck | Yes |
| GET | `/api/decks/{id}/validate` | Validate deck rules | Yes |
| GET | `/api/decks/public` | Get public decks | No |

### Image Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/images/{cardId}/regular` | Get full-size card image | No |
| GET | `/api/images/{cardId}/small` | Get thumbnail card image | No |
| GET | `/api/images/{cardId}/exists` | Check if image exists | No |

### Example Requests

<details>
<summary><b>Register User</b></summary>

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "duelist123",
    "email": "duelist@example.com",
    "password": "SecurePass123!"
  }'
```

Response:
```json
{
  "success": true,
  "message": "Registration successful",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "duelist123",
    "email": "duelist@example.com",
    "createdAt": "2024-01-15T10:30:00"
  }
}
```
</details>

<details>
<summary><b>Search Card</b></summary>

```bash
curl "http://localhost:8080/api/cards/search?name=Blue-Eyes%20White%20Dragon"
```

Response:
```json
{
  "success": true,
  "message": "Card found",
  "data": {
    "id": 89631139,
    "name": "Blue-Eyes White Dragon",
    "type": "Normal Monster",
    "frameType": "normal",
    "desc": "This legendary dragon is a powerful engine of destruction...",
    "atk": 3000,
    "def": 2500,
    "level": 8,
    "race": "Dragon",
    "attribute": "LIGHT",
    "imageUrl": "http://localhost:8080/api/images/89631139/regular",
    "imageUrlSmall": "http://localhost:8080/api/images/89631139/small",
    "prices": {
      "cardmarketPrice": "0.15",
      "tcgplayerPrice": "0.25",
      "coolstuffincPrice": "0.49"
    }
  }
}
```
</details>

<details>
<summary><b>Create Deck</b></summary>

```bash
curl -X POST http://localhost:8080/api/decks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "name": "Blue-Eyes Deck",
    "description": "Classic Blue-Eyes White Dragon beatdown deck",
    "isPublic": false
  }'
```

Response:
```json
{
  "success": true,
  "message": "Deck created successfully",
  "data": {
    "id": 1,
    "name": "Blue-Eyes Deck",
    "description": "Classic Blue-Eyes White Dragon beatdown deck",
    "isPublic": false,
    "ownerUsername": "duelist123",
    "mainDeckCount": 0,
    "extraDeckCount": 0,
    "sideDeckCount": 0,
    "totalCards": 0,
    "isValid": false,
    "validationErrors": ["Main Deck must have at least 40 cards (currently 0)"]
  }
}
```
</details>

---

## 🗄 Database Schema

```
┌─────────────────────┐      ┌─────────────────────┐
│       users         │      │    user_decks       │
├─────────────────────┤      ├─────────────────────┤
│ id (PK)             │──┐   │ id (PK)             │
│ username (UNIQUE)   │  │   │ user_id (FK)────────┼───┐
│ email (UNIQUE)      │  │   │ name                │   │
│ password_hash       │  │   │ description         │   │
│ created_at          │  │   │ is_public           │   │
│ updated_at          │  │   │ created_at          │   │
└─────────────────────┘  │   │ updated_at          │   │
                         │   └─────────────────────┘   │
                         │             │               │
                         └─────────────┼───────────────┘
                                       │
                         ┌─────────────▼───────────────┐
                         │       deck_cards            │
                         ├─────────────────────────────┤
                         │ id (PK)                     │
                         │ deck_id (FK)                │
                         │ card_id                     │
                         │ quantity                    │
                         │ deck_type (MAIN/EXTRA/SIDE) │
                         │ added_at                    │
                         └─────────────────────────────┘
                                       │
┌─────────────────────┐               │
│       cards         │               │
├─────────────────────┤               │
│ id (PK)             │               │
│ card_id (UNIQUE)◄───┼───────────────┘
│ name                │
│ type                │
│ frame_type          │
│ description         │      ┌─────────────────────┐
│ atk                 │      │    card_images      │
│ def                 │      ├─────────────────────┤
│ level               │      │ id (PK)             │
│ race                │      │ card_id (FK)────────┼───┐
│ attribute           │      │ image_data (BYTEA)  │   │
│ card_sets (JSONB)   │      │ image_small (BYTEA) │   │
│ card_prices (JSONB) │◄─────┼───────────────────────────┘
│ created_at          │      │ content_type        │
│ updated_at          │      │ file_size           │
└─────────────────────┘      │ original_url        │
                             │ downloaded_at       │
┌─────────────────────┐      └─────────────────────┘
│password_reset_tokens│
├─────────────────────┤
│ id (PK)             │
│ token (UNIQUE)      │
│ email               │
│ expiry_date         │
│ used                │
│ created_at          │
└─────────────────────┘
```

---

## 🔒 Security Implementation

### Authentication & Authorization

- **JWT-based Authentication**: Stateless authentication using access and refresh tokens
- **Token Types**: Separate access (15min) and refresh (7 days) tokens
- **Password Hashing**: BCrypt with strength factor 12
- **Secure Password Requirements**: Minimum 8 characters with uppercase, lowercase, number, and special character

### Security Filters (in order)

1. **Rate Limit Filter**: Prevents brute-force attacks
   - General endpoints: 60 requests/minute
   - Auth endpoints: 10 requests/minute

2. **JWT Authentication Filter**: Validates Bearer tokens

3. **Security Headers Filter**: Adds protective HTTP headers
   - `X-Frame-Options: DENY`
   - `X-Content-Type-Options: nosniff`
   - `X-XSS-Protection: 1; mode=block`
   - `Referrer-Policy: strict-origin-when-cross-origin`

### CORS Configuration

- Configurable allowed origins via environment variable
- Supports credentials for authenticated requests
- Preflight request caching (1 hour)

### Data Protection

- Password reset tokens expire after 1 hour
- Automatic cleanup of expired tokens (hourly scheduled task)
- No sensitive data in JWT payload
- Email enumeration protection (consistent responses)

---

## 🌐 Deployment

### Heroku Deployment

1. **Create Heroku app**
   ```bash
   heroku create your-app-name
   ```

2. **Add PostgreSQL addon**
   ```bash
   heroku addons:create heroku-postgresql:essential-0
   ```

3. **Set environment variables**
   ```bash
   heroku config:set JWT_SECRET=your-256-bit-secret
   heroku config:set APP_PYTHON_OCR_URL=https://your-ocr-service.hf.space
   heroku config:set APP_BACKEND_URL=https://your-app-name.herokuapp.com
   heroku config:set APP_FRONTEND_URL=https://your-frontend.netlify.app
   heroku config:set APP_CORS_ALLOWED_ORIGINS=https://your-frontend.netlify.app
   heroku config:set SPRING_MAIL_HOST=smtp.gmail.com
   heroku config:set SPRING_MAIL_USERNAME=your-email@gmail.com
   heroku config:set SPRING_MAIL_PASSWORD=your-app-password
   ```

4. **Deploy**
   ```bash
   git push heroku main
   ```

### Docker Compose (Local Development)

```yaml
version: '3.8'
services:
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=postgres://postgres:postgres@db:5432/yugioh
      - JWT_SECRET=your-development-secret-key-here-min-32-chars
      - APP_PYTHON_OCR_URL=http://ocr:5000
    depends_on:
      - db

  db:
    image: postgres:15
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
      - POSTGRES_DB=yugioh
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

---

## 🔮 Future Enhancements

- [ ] **Card Collection Tracker**: Track owned cards with condition and quantity
- [ ] **Deck Statistics**: Win rate tracking and meta analysis
- [ ] **Card Recommendations**: AI-powered deck building suggestions
- [ ] **Trade System**: Trade cards with other users
- [ ] **Mobile App**: React Native mobile application
- [ ] **Import/Export**: YDK file support for YGOPRO compatibility
- [ ] **Tournament Mode**: Deck registration and validation for tournaments
- [ ] **Price Alerts**: Notifications when card prices change
- [ ] **Social Features**: Follow users, like decks, comments

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [YGOPRODeck](https://ygoprodeck.com/) for providing the card database API
- [Konami](https://www.konami.com/) for creating Yu-Gi-Oh!
---
