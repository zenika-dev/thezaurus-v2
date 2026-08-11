from talk_agent.agent import parse_talk_payload, build_talk_prompt

payload = {
    'title': 'Optimiser les agents IA',
    'abstract': 'Cet article propose un cadre pour concevoir des agents avec une meilleure qualité de relecture technique.'
}

data = parse_talk_payload(payload)
assert data.title == payload['title']
assert data.abstract == payload['abstract']
text = build_talk_prompt(data)
assert 'Titre du talk :' in text
assert 'Abstract du talk :' in text
print('JSON API smoke test OK')
