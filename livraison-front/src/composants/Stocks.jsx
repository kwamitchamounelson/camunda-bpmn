import { useState } from 'react';
import { api } from '../api.js';
import { euros, vignette } from '../domaine.js';
import { Badge, Bouton, Carte } from './ui.jsx';

function LigneStock({ article, executer }) {
  const [valeur, setValeur] = useState(String(article.quantite));
  const rupture = article.quantite <= 0;

  return (
    <tr>
      <td className="stock__produit">
        <span aria-hidden="true">{vignette(article.produit)}</span>
        {article.produit}
      </td>
      <td>{euros(article.prixUnitaire)}</td>
      <td>
        <strong>{article.quantite}</strong>{' '}
        {rupture && <Badge ton="alerte">rupture</Badge>}
      </td>
      <td className="stock__actions">
        <Bouton
          onClick={() =>
            executer(
              () => api.mouvementStock(article.produit, -1),
              (maj) => `${maj.produit} : ${maj.quantite} en stock`,
            )
          }
        >
          −1
        </Bouton>
        <Bouton
          onClick={() =>
            executer(
              () => api.mouvementStock(article.produit, 10),
              (maj) => `${maj.produit} : ${maj.quantite} en stock`,
            )
          }
        >
          +10
        </Bouton>
        <input
          type="number"
          min="0"
          value={valeur}
          aria-label={`Nouvelle quantité pour ${article.produit}`}
          onChange={(e) => setValeur(e.target.value)}
        />
        <Bouton
          variante="principal"
          onClick={() =>
            executer(
              () => api.definirStock(article.produit, Number(valeur)),
              (maj) => `${maj.produit} fixé à ${maj.quantite}`,
            )
          }
        >
          Fixer
        </Bouton>
      </td>
    </tr>
  );
}

export default function Stocks({ stocks, executer }) {
  return (
    <Carte titre="Stock du magasin">
      <p className="texte-doux">
        Mettre un produit à 0 force la rupture : l'instance boucle sur le réapprovisionnement avec un
        timer de 30 secondes. Remonter la quantité pendant l'attente la débloque au contrôle suivant.
      </p>
      <table className="tableau">
        <thead>
          <tr>
            <th>Produit</th>
            <th>Prix unitaire</th>
            <th>Disponible</th>
            <th>Ajuster</th>
          </tr>
        </thead>
        <tbody>
          {stocks.map((article) => (
            <LigneStock key={article.produit} article={article} executer={executer} />
          ))}
        </tbody>
      </table>
    </Carte>
  );
}
