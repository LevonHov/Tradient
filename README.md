# Tradient - Cryptocurrency Arbitrage Trading Platform

## Project Overview

Tradient is an advanced cryptocurrency arbitrage trading platform that identifies and evaluates profitable trading opportunities across multiple exchanges in real-time. The system uses sophisticated algorithms to detect price discrepancies between exchanges and provides comprehensive risk assessments based on actual market conditions.

## Key Features

### Real-Time Arbitrage Detection
- Identifies price differences between cryptocurrency exchanges
- Calculates potential profit after accounting for fees and slippage
- Monitors thousands of trading pairs continuously
- Prioritizes high-opportunity pairs through dynamic symbol selection

### Advanced Risk Assessment System
- Uses real market data instead of estimates or simulations
- Calculates liquidity scores based on actual trading volumes
- Measures market depth from order books to determine realistic slippage
- Analyzes historical volatility to predict price stability
- Estimates execution time based on exchange performance and market conditions
- Provides ROI efficiency metrics (profit per hour)

### Exchange Integration Framework
- Standardized API adapter interface for consistent exchange access
- Real-time data collection via REST APIs and WebSockets
- Currently supports major exchanges including Binance, Coinbase, Kraken, OKX, and more
- Optimized network handling with proper timeouts and error recovery

### User-Friendly Interface
- Clear risk level indicators (from "VERY LOW RISK" to "EXTREME RISK")
- Visual representations of trading opportunities
- Detailed market data visualization
- Performance statistics and history

## Technical Implementation

### Data Flow Architecture
1. Exchange API adapters fetch real-time market data from multiple sources
2. Order books and ticker data are normalized into a standard format
3. Arbitrage detection engine identifies potential opportunities
4. RealTimeRiskCalculator evaluates each opportunity using market metrics
5. Results are displayed to the user with actionable information

### Risk Calculation Components
- **Liquidity Analysis**: Evaluates trading volumes on both exchanges
- **Volatility Measurement**: Analyzes historical price movements to assess stability
- **Market Depth Assessment**: Examines order books to evaluate slippage potential
- **Execution Time Estimation**: Predicts how long trades will take to complete
- **Exchange Risk Evaluation**: Considers the reliability and security of exchanges

### Core Technologies
- Java/Android for mobile application development
- OkHttp for efficient network communication
- Concurrent programming with CompletableFuture for non-blocking operations
- Reflection API for flexible handling of different exchange data models

## Getting Started

### Prerequisites
- Android Studio 4.0+
- JDK 11+
- Exchange API keys (for live trading)

### Installation
1. Clone the repository
2. Import the project into Android Studio
3. Configure your exchange API keys in the settings
4. Build and run the application

## Improvement Roadmap

### Recent Improvements
- Enhanced risk calculation with real-time market data
- Improved slippage estimation using actual order book depth
- Added detailed log output for better tracking of calculation factors
- Increased network timeout handling for more reliable exchange connections

### Planned Enhancements
1. WebSocket connections for all supported exchanges
2. Dynamic symbol selection based on opportunity metrics
3. Enhanced performance through optimized algorithms
4. Additional arbitrage techniques (triangular, statistical)
5. Microservice architecture for scalability
6. Exchange specialization focus

## License

This project is proprietary software. All rights reserved.

## Acknowledgements

- Built with the support of the cryptocurrency trading community
- Special thanks to all contributors who have helped improve the platform 