# Tradient - UI/UX Design Documentation

![Tradient Logo](assets/logo.png)

<div style="background-color: #121621; color: #e0e0e0; padding: 20px; border-radius: 8px; border-left: 5px solid #0052FF;">
<h2 style="color: #0052FF; text-shadow: 0 0 5px #0052FF;">Tradient Design System</h2>
<p style="color: #e0e0e0;">A comprehensive guide to the visual language, components, and interaction patterns of the Tradient crypto arbitrage platform.</p>
</div>

## Table of Contents

1. [Brand Identity](#1-brand-identity)
2. [Color System](#2-color-system)
3. [Typography](#3-typography)
4. [Iconography & Imagery](#4-iconography--imagery)
5. [Component Library](#5-component-library)
6. [Screen Layouts](#6-screen-layouts)
7. [Navigation Patterns](#7-navigation-patterns)
8. [Animation & Motion](#8-animation--motion)
9. [Data Visualization](#9-data-visualization)
10. [Accessibility Guidelines](#10-accessibility-guidelines)
11. [Design Principles](#11-design-principles)

## 1. Brand Identity

### Logo

The Tradient logo embodies the concept of arbitrage through a stylized "T" formed by two market arrows (ascending and descending) creating a bridge-like shape. The negative space between arrows creates the distinctive "T" silhouette.

#### Logo Variations

| Variation | Usage |
|-----------|-------|
| Full Logo (Horizontal) | App header, marketing materials, splash screen |
| Symbol Only | App icon, favicon, UI elements |
| Wordmark Only | Documentation, text-only contexts |
| Monochrome | Single-color applications |

#### Logo Guidelines

- **Clear Space**: Maintain clear space equal to the height of the "T" symbol on all sides
- **Minimum Size**: 24dp height for digital, 10mm for print
- **Do Not**: Stretch, rotate, recolor outside of approved palette, or add effects

### Brand Voice

- **Professional yet Approachable**: Technical information presented in an accessible manner
- **Confident and Trustworthy**: Establishes authority in financial matters while being transparent
- **Clear and Concise**: Avoids jargon and communicates complex concepts simply
- **Empowering**: Focuses on enabling users to make informed decisions

## 2. Color System

### Primary Colors

| Color | Hex Code | Usage |
|-------|----------|-------|
| ![Neon Blue](https://via.placeholder.com/15/0052FF/0052FF) Neon Blue | `#0052FF` | Primary brand color, CTA buttons, key indicators |
| ![Dark Blue Variant](https://via.placeholder.com/15/003CBB/003CBB) Dark Blue Variant | `#003CBB` | Pressed states, secondary elements |

### Secondary Colors

| Color | Hex Code | Usage |
|-------|----------|-------|
| ![Electric Purple](https://via.placeholder.com/15/9945FF/9945FF) Electric Purple | `#9945FF` | Accent elements, secondary CTAs, highlights |

### Functional Colors

| Color | Hex Code | Usage |
|-------|----------|-------|
| ![Success Green](https://via.placeholder.com/15/00C087/00C087) Success Green | `#00C087` | Profits, successful actions, positive indicators |
| ![Alert Orange](https://via.placeholder.com/15/FF9332/FF9332) Alert Orange | `#FF9332` | Warnings, medium-risk indicators |
| ![Error Red](https://via.placeholder.com/15/FF3B30/FF3B30) Error Red | `#FF3B30` | Errors, high-risk indicators, negative outcomes |

### Neutrals

| Color | Hex Code | Usage |
|-------|----------|-------|
| ![Dark Background](https://via.placeholder.com/15/121621/121621) Dark Background | `#121621` | Main app background |
| ![Card Background](https://via.placeholder.com/15/1E2130/1E2130) Card Background | `#1E2130` | Cards, dialogs, surfaces |
| ![Input Field Background](https://via.placeholder.com/15/2A2F42/2A2F42) Input Field | `#2A2F42` | Form fields, search bars |
| ![Status Bar](https://via.placeholder.com/15/0F121B/0F121B) Status Bar | `#0F121B` | Status bar background |
| ![Neutral Gray](https://via.placeholder.com/15/8A8D98/8A8D98) Neutral Gray | `#8A8D98` | Secondary text, inactive elements |

### Text Colors

| Color | Hex Code | Usage |
|-------|----------|-------|
| ![Primary Text](https://via.placeholder.com/15/FFFFFF/FFFFFF) Primary Text | `#FFFFFF` | Main text on dark backgrounds |
| ![Secondary Text](https://via.placeholder.com/15/B4B6BD/B4B6BD) Secondary Text | `#B4B6BD` | Less prominent text |
| ![Disabled Text](https://via.placeholder.com/15/646876/646876) Disabled Text | `#646876` | Inactive text elements |

### Gradient Palettes

| Gradient | Colors | Usage |
|----------|--------|-------|
| Brand Gradient | `#0052FF` → `#9945FF` | Feature highlights, special UI elements |
| Profit Gradient | `#00C087` → `#0052FF` | Profit visualization |
| Risk Gradient | `#00C087` → `#FF9332` → `#FF3B30` | Risk indicators |

### Color Application Rules

- Use Neon Blue sparingly to highlight important actions or information
- Maintain sufficient contrast between text and backgrounds (minimum 4.5:1)
- Apply functional colors consistently for their semantic meaning
- Use gradients for emphasis or to indicate progression, not for standard UI elements

## 3. Typography

### Font Families

- **Primary Font**: Inter
  - Clean, modern sans-serif with excellent readability
  - Used for all UI text elements
  
- **Monospace Font**: Roboto Mono
  - Used for price data, percentages, and financial metrics
  - Creates visual alignment for numerical data

### Type Scale

| Size (dp) | Weight | Line Height | Usage |
|-----------|--------|-------------|-------|
| 28 | Bold (700) | 34dp | Display (Main Screen Titles) |
| 24 | Bold (700) | 29dp | Headline (Major Section Headers) |
| 20 | Semi-Bold (600) | 26dp | Title (Card Titles) |
| 16 | Regular (400) | 24dp | Body (Standard Text) |
| 16 | Medium (500) | 24dp | Body Emphasis |
| 14 | Regular (400) | 21dp | Subtext (Secondary Info) |
| 14 | Medium (500) | 21dp | Subtext Emphasis |
| 12 | Regular (400) | 17dp | Caption (Fine Print) |

### Font Weights

| Weight | Usage |
|--------|-------|
| Bold (700) | App title, section headers, profit percentages |
| Semi-Bold (600) | Tab labels, button text, important values |
| Medium (500) | Menu items, exchange names, emphasized text |
| Regular (400) | Description text, normal content |

### Text Styles

#### Special Treatments

- **Monospace Numbers**: All numerical trading data, prices, and percentages
- **All Caps**: Small labels, categories, and section dividers (letter spacing +0.5px)
- **Single Line Ellipsis**: Truncate long exchange names and asset pairs with ellipsis
- **Field Labels**: 14dp Medium, placed above input fields, Secondary Text color

#### Text Alignment

- Left-align most text for readability
- Center-align button text and section headers
- Right-align numerical data in tables and comparison views

## 4. Iconography & Imagery

### Icon System

#### Style Guidelines

- **Two-tone Material Design** based icons with Tradient-specific customizations
- Line weight of 1.5-2px for outlined icons
- Consistent corner radius of 2px for custom icons
- Maintain clear silhouettes recognizable at small sizes

#### Icon Sizes

| Size (dp) | Usage |
|-----------|-------|
| 24×24 | Standard navigation and action icons |
| 20×20 | Tab bar and smaller functional icons |
| 16×16 | Inline icons within text |
| 32×32 | Featured/prominent icons |

#### Custom Icon Categories

| Category | Examples | Usage |
|----------|----------|-------|
| Navigation | Home, Opportunities, Trades, Settings | Main navigation areas |
| Exchange | Binance, Coinbase, Kraken, Bybit icons | Exchange identification |
| Trading | Buy, Sell, Transfer, Portfolio | Trading actions |
| Indicators | Risk, Profit, Time, Success Rate | Status indicators |
| Arbitrage | Opportunity, Cycle, Bridge, Gap | Arbitrage-specific concepts |

### Illustration Style

- Geometric and minimalist illustrations with clear purpose
- Limited color palette focusing on brand colors
- Uniform line weight and drawing style
- Subtle gradient overlays for depth
- Low-detail isometric style for 3D representation when needed

### Photography & Image Treatment

- High-contrast, dark-themed financial imagery when needed
- Circuit/network patterns for backgrounds representing connectivity
- Abstract data visualization patterns for empty states
- Blue gradient color overlay for consistency on imported images

## 5. Component Library

### Core Components

#### Buttons

| Type | Appearance | States | Usage |
|------|------------|--------|-------|
| Primary | Filled, rounded (4dp), Neon Blue | Normal, Hover, Pressed, Loading, Disabled | Main actions, confirmations |
| Secondary | Outlined, rounded (4dp), White border | Normal, Hover, Pressed, Disabled | Alternative actions |
| Text | No background, Neon Blue text | Normal, Hover, Pressed, Disabled | Subtle actions, links |
| Icon | Circle, Neon Blue or transparent | Normal, Hover, Pressed, Disabled | Compact actions |
| Floating Action | Circle, Neon Blue with shadow | Normal, Pressed, Extended | Primary screen action |

#### Input Fields

| Type | Appearance | States | Features |
|------|------------|--------|----------|
| Text Input | Rounded (4dp), Input Field Background | Normal, Focus, Error, Disabled | Character count, clear button |
| Search Field | Rounded (4dp), Icon prefix | Normal, Focus, Active, Empty | Autocomplete, recent searches |
| Number Input | Rounded (4dp), monospace text | Normal, Focus, Error, Disabled | Increment/decrement buttons |
| Dropdown | Rounded (4dp), trailing icon | Normal, Expanded, Selected, Disabled | Option list, search filter |
| Slider | Track with filled section, round knob | Normal, Dragging, Disabled | Value tooltip, custom range |

#### Selection Controls

| Type | Appearance | States | Usage |
|------|------------|--------|-------|
| Checkbox | Square with rounded corners | Unchecked, Checked, Indeterminate, Disabled | Multiple selection |
| Radio Button | Circle with filled center | Unselected, Selected, Disabled | Single selection |
| Toggle Switch | Pill-shaped track with round knob | Off, On, Disabled | Binary settings |
| Chips | Rounded pill, subtle background | Unselected, Selected, Disabled | Filtering, tags |
| Segmented Control | Connected buttons group | Unselected, Selected, Disabled | View switching |

### Specialized Components

#### Opportunity Card

A specialized card component for displaying arbitrage opportunities with:
- Visual profit indicator (colored based on profitability)
- Risk assessment meter
- Exchange pair visualization with logos
- Time sensitivity indicator
- Quick-action buttons
- Expandable details section

#### Risk Gauge

A custom semi-circular gauge that displays:
- Risk level from low to high
- Color-coded zones (green to red)
- Needle indicator showing current risk assessment
- Numerical risk score below
- Tooltip with risk breakdown on hover

#### Profit Timeline

A specialized chart component showing:
- Arbitrage opportunity duration
- Entry and exit points with exchange icons
- Profit projection with confidence interval
- Success probability indicator
- Historical similar opportunities (optional)

#### Market Spread Visualizer

A custom component visualizing:
- Price gap between exchanges
- Historical spread trend (line chart)
- Volatility indicators (shaded area)
- Best execution points (highlighted)
- Current market position

#### Exchange Connection Card

Displays:
- Exchange logo and name
- Connection status indicator (colored dot)
- Last sync time
- Available balance summary
- Quick action buttons (sync, settings)
- Health metrics (latency, uptime)

## 6. Screen Layouts

### Core Screens

#### Dashboard 