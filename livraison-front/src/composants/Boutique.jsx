import { useState } from 'react';
import { api } from '../api.js';
import { euros, vignette } from '../domaine.js';
import { Badge, Bouton, Carte, Champ } from './ui.jsx';

const CLIENT_INITIAL = { client: 'Alice', email: 'alice@exemple.fr', typeClient: 'STANDARD' };

export default function Boutique({ stocks, executer, surCommandeCreee }) {
  const [client, setClient] = useState(CLIENT_INITIAL);
  const [quantites, setQuantites] = useState({});
  const [enCours, setEnCours] = useState(null);

  const quantiteDe = (produit) => quantites[produit] ?? 1;

  const majQuantite = (produit, valeur) =>
    setQuantites((actuel) => ({ ...actuel, [produit]: Math.max(1, valeur) }));

  async function commander(produit) {
    setEnCours(produit);
    const commande = await executer(
      () =>
        api.creerCommande({
          client: client.client,
          email: client.email,
          produit,
          quantite: quantiteDe(produit),
          typeClient: client.typeClient,
        }),
      (creee) => `Commande ${creee.id} créée — le processus est démarré`,
    );
    setEnCours(null);
    if (commande) surCommandeCreee(commande);
  }

  return (
    <>
      <Carte titre="Qui commande ?" className="carte--client">
        <div className="grille-formulaire">
          <Champ label="Nom du client">
            <input
              value={client.client}
              onChange={(e) => setClient({ ...client, client: e.target.value })}
            />
          </Champ>
          <Champ label="Email">
            <input
              type="email"
              value={client.email}
              onChange={(e) => setClient({ ...client, email: e.target.value })}
            />
          </Champ>
          <Champ label="Type de client" indice="VIP saute la validation en dessous de 10 000 €">
            <select
              value={client.typeClient}
              onChange={(e) => setClient({ ...client, typeClient: e.target.value })}
            >
              <option value="STANDARD">Standard</option>
              <option value="VIP">VIP</option>
            </select>
          </Champ>
        </div>
      </Carte>

      <div className="grille-produits">
        {stocks.map((article) => {
          const rupture = article.quantite <= 0;
          const quantite = quantiteDe(article.produit);
          const insuffisant = !rupture && quantite > article.quantite;

          return (
            <article key={article.produit} className="produit">
              <div className="produit__image">{vignette(article.produit)}</div>
              <h3 className="produit__nom">{article.produit}</h3>
              <p className="produit__prix">{euros(article.prixUnitaire)}</p>

              <div className="produit__stock">
                {rupture ? (
                  <Badge ton="alerte">Rupture de stock</Badge>
                ) : (
                  <Badge ton={article.quantite < 5 ? 'alerte' : 'succes'}>
                    {article.quantite} en stock
                  </Badge>
                )}
              </div>

              <div className="produit__achat">
                <input
                  type="number"
                  min="1"
                  value={quantite}
                  aria-label={`Quantité de ${article.produit}`}
                  onChange={(e) => majQuantite(article.produit, Number(e.target.value))}
                />
                <Bouton
                  variante="principal"
                  disabled={enCours === article.produit}
                  onClick={() => commander(article.produit)}
                >
                  {enCours === article.produit ? 'Envoi…' : 'Commander'}
                </Bouton>
              </div>

              <p className="produit__note">
                {rupture
                  ? 'Commandable : le processus partira en réapprovisionnement.'
                  : insuffisant
                    ? `Au-delà du stock : rupture puis réapprovisionnement.`
                    : `Total ${euros(article.prixUnitaire * quantite)}`}
              </p>
            </article>
          );
        })}
      </div>
    </>
  );
}
