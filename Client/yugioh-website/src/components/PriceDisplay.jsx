import React from 'react';
import { ExternalLink } from 'lucide-react';

/**
 * Marketplace URLs for price links
 */
const MARKETPLACE_URLS = {
    cardmarket: 'https://www.cardmarket.com/en/YuGiOh/Products/Search?searchString=',
    tcgplayer: 'https://www.tcgplayer.com/search/yugioh/product?productLineName=yugioh&q=',
    coolstuffinc: 'https://www.coolstuffinc.com/main_search.php?pa=searchOnName&page=1&resultsPerPage=25&q=',
};

/**
 * Format price with currency symbol
 */
const formatPrice = (price, currency = '$') => {
    if (!price || price === '0' || price === 0) return null;
    const numPrice = typeof price === 'string' ? parseFloat(price) : price;
    if (isNaN(numPrice) || numPrice === 0) return null;
    return `${currency}${numPrice.toFixed(2)}`;
};

/**
 * Single price badge with link
 */
const PriceBadge = ({ label, price, currency, url, colorClass }) => {
    const formattedPrice = formatPrice(price, currency);
    if (!formattedPrice) return null;

    return (
        <a
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            className={`price-badge ${colorClass}`}
            title={`View on ${label}`}
        >
            <span className="price-label">{label}:</span>
            <span className="price-value">{formattedPrice}</span>
            <ExternalLink size={12} className="price-link-icon" />
        </a>
    );
};

/**
 * Display prices for a single card with marketplace links
 */
export const CardPriceDisplay = ({ prices, cardName }) => {
    if (!prices) return null;

    const encodedName = encodeURIComponent(cardName || '');

    const hasAnyPrice =
        (prices.cardmarketPrice && prices.cardmarketPrice !== '0') ||
        (prices.tcgplayerPrice && prices.tcgplayerPrice !== '0') ||
        (prices.coolstuffincPrice && prices.coolstuffincPrice !== '0');

    if (!hasAnyPrice) return null;

    return (
        <div className="card-prices">
            <PriceBadge
                label="Cardmarket"
                price={prices.cardmarketPrice}
                currency="€"
                url={`${MARKETPLACE_URLS.cardmarket}${encodedName}`}
                colorClass="cardmarket"
            />
            <PriceBadge
                label="TCGPlayer"
                price={prices.tcgplayerPrice}
                currency="$"
                url={`${MARKETPLACE_URLS.tcgplayer}${encodedName}`}
                colorClass="tcgplayer"
            />
            <PriceBadge
                label="CoolStuffInc"
                price={prices.coolstuffincPrice}
                currency="$"
                url={`${MARKETPLACE_URLS.coolstuffinc}${encodedName}`}
                colorClass="coolstuffinc"
            />
        </div>
    );
};

/**
 * Display total prices for a deck
 */
export const DeckPriceDisplay = ({ totalPrices, deckName }) => {
    if (!totalPrices) return null;

    const { cardmarketTotal, tcgplayerTotal, coolstuffincTotal } = totalPrices;

    const hasAnyPrice = cardmarketTotal > 0 || tcgplayerTotal > 0 || coolstuffincTotal > 0;

    if (!hasAnyPrice) return null;

    return (
        <div className="deck-prices">
            <h4 className="deck-prices-title">Estimated Deck Value</h4>
            <div className="deck-price-badges">
                {cardmarketTotal > 0 && (
                    <div className="deck-price-badge cardmarket">
                        <span className="price-label">Cardmarket</span>
                        <span className="price-value">€{cardmarketTotal.toFixed(2)}</span>
                    </div>
                )}
                {tcgplayerTotal > 0 && (
                    <div className="deck-price-badge tcgplayer">
                        <span className="price-label">TCGPlayer</span>
                        <span className="price-value">${tcgplayerTotal.toFixed(2)}</span>
                    </div>
                )}
                {coolstuffincTotal > 0 && (
                    <div className="deck-price-badge coolstuffinc">
                        <span className="price-label">CoolStuffInc</span>
                        <span className="price-value">${coolstuffincTotal.toFixed(2)}</span>
                    </div>
                )}
            </div>
        </div>
    );
};

/**
 * Compact price display for card lists (deck view)
 */
export const CompactPriceDisplay = ({ prices, cardName, quantity = 1 }) => {
    if (!prices) return null;

    const encodedName = encodeURIComponent(cardName || '');

    // Show TCGPlayer price by default (most common for US users)
    const price = prices.tcgplayerPrice || prices.cardmarketPrice;
    const currency = prices.tcgplayerPrice ? '$' : '€';
    const url = prices.tcgplayerPrice
        ? `${MARKETPLACE_URLS.tcgplayer}${encodedName}`
        : `${MARKETPLACE_URLS.cardmarket}${encodedName}`;

    if (!price || price === '0') return null;

    const numPrice = parseFloat(price);
    if (isNaN(numPrice) || numPrice === 0) return null;

    const totalPrice = numPrice * quantity;

    return (
        <a
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            className="compact-price"
            title={`${quantity}x @ ${currency}${numPrice.toFixed(2)} each`}
        >
            {currency}{totalPrice.toFixed(2)}
        </a>
    );
};

export default CardPriceDisplay;