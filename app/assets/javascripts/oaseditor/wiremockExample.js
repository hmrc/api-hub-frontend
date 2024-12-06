export const exampleWiremock = `mappings:
  boardgames-delete-notfound.json: >
    {
      "request": {
        "method": "DELETE",
        "urlPattern": "/backend/boardgames/[0-9]+"
      },
      "response": {
        "status": 404,
        "bodyFileName": "boardgame-response.json",
        "headers": {
          "Content-Type": "application/json"
        }
      }
    }
files:
  boardgame-response.json: >
    {
      "id": 1,
      "name": "Exploding Kittens",
      "category": {       
        "id": 545,
        "name": "Card Games"
      },
      "photoUrls": [       
        "string"
      ],
      "tags": [
        {
          "id": 1,
          "name": "Most Popular"
        }
      ],
      "status": "available"
    }
`;