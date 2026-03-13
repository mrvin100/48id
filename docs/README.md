# 48ID Documentation

Welcome to the 48ID documentation. This guide will help you understand, integrate with, and contribute to 48ID.

## Documentation Structure

```
docs/
├── guide/              # User guides and concepts
│   ├── introduction.md
│   ├── quickstart.md
│   ├── architecture.md
│   ├── authentication.md
│   ├── integration.md
│   └── deployment.md
├── api/                # API reference
│   ├── overview.md
│   ├── authentication.md
│   ├── identity.md
│   ├── admin.md
│   ├── integration.md
│   └── errors.md
├── developers/         # Developer documentation
│   ├── contributing.md
│   └── story-workflow.md
└── GLOSSARY.md         # Terms and concepts
```

## Quick Navigation

### Getting Started

- 🚀 **[Quick Start](guide/quickstart.md)** — Get 48ID running in 5 minutes
- 📖 **[Introduction](guide/introduction.md)** — What is 48ID and the K48 ecosystem
- 🏗️ **[Architecture](guide/architecture.md)** — System design and module structure

### Using 48ID

- 🔐 **[Authentication Guide](guide/authentication.md)** — How authentication works
- 🔌 **[Integration Guide](guide/integration.md)** — Integrate your application
- 🚢 **[Deployment Guide](guide/deployment.md)** — Deploy to production

### API Reference

- 📚 **[API Overview](api/overview.md)** — API introduction and conventions
- 🔑 **[Authentication API](api/authentication.md)** — Login, tokens, password flows
- 👤 **[Identity API](api/identity.md)** — User profile operations
- 🔧 **[Admin API](api/admin.md)** — User management, audit, API keys
- 🔗 **[Integration API](api/integration.md)** — Token verification, public identity
- ❌ **[Error Reference](api/errors.md)** — Error codes and handling

### For Developers

- 🛠️ **[Contributing](developers/contributing.md)** — Development setup and standards
- 📝 **[Story Workflow](developers/story-workflow.md)** — Implementing backlog stories
- 📖 **[Glossary](GLOSSARY.md)** — Terms and concepts

## Common Use Cases

### I want to...

**...integrate my app with 48ID for user authentication**
→ Read: [Integration Guide](guide/integration.md) → [Authentication API](api/authentication.md)

**...understand how authentication works**
→ Read: [Authentication Guide](guide/authentication.md)

**...deploy 48ID to production**
→ Read: [Deployment Guide](guide/deployment.md)

**...verify user tokens from my backend**
→ Read: [Integration Guide](guide/integration.md#pattern-2-backend-service-integration) → [Integration API](api/integration.md)

**...manage users as an admin**
→ Read: [Admin API](api/admin.md)

**...contribute to the project**
→ Read: [Contributing](developers/contributing.md) → [Story Workflow](developers/story-workflow.md)

## Documentation Principles

This documentation is:

✅ **Implementation-aligned** — Describes actual behavior, not planned features  
✅ **MVP-scoped** — Covers current MVP, with notes on future enhancements  
✅ **Modular** — Easy to navigate and extend  
✅ **Example-rich** — Includes code examples and diagrams  
✅ **Up-to-date** — Updated with every feature change  

## Need Help?

- 📖 Check this documentation
- 💬 Open a GitHub Discussion
- 🐛 Report bugs via GitHub Issues
- 📧 Contact K48 administration for operational support

---

**[⬆ Back to main README](../README.md)**
