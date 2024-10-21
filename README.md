# TechNotify

Introduction:-

The TechNotify is a web application designed to aggregate and display technology-related news from various sources. It also includes updates on programming language releases, allowing users to stay informed about the latest developments in the tech industry. Users can post new content and engage in discussions, making it a community-driven platform.

Technologies Used:-

• Backend: o Spring Boot o Spring Security o Spring Batch o JPA o MongoDB o MySQL

• Frontend: o React.js

• Architecture: o Microservices

Project Features:-

• News Aggregation: Fetches and displays tech-related news from multiple websites. • Programming Language Updates: Notifies users about new releases of programming languages. • User-Generated Content: Allows users to post and share their own articles. • Social Features: Includes options for users to like, share, and discuss articles. • Authentication and Security: Implements user authentication and role-based access control. • Batch Processing: Utilizes Spring Batch for efficient data processing and fetching.

Architecture:- The architecture consists of multiple microservices communicating through REST APIs. The backend handles data aggregation, storage, and user interactions, while the React.js frontend provides a responsive and dynamic user interface.

Microservices Breakdown:-

• News Service: Responsible for fetching and storing news articles. • User Service: Manages user authentication and profiles. • Content Service: Handles user-generated content and interactions. • Notification Service: Sends updates about new programming language releases.
