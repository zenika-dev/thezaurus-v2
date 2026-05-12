import * as React from 'react';
import Button from '@mui/material/Button';
import DialogTitle from '@mui/material/DialogTitle';
import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Switch from '@mui/material/Switch';
import FormControlLabel from '@mui/material/FormControlLabel';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select, { type SelectChangeEvent } from '@mui/material/Select';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import Snackbar from '@mui/material/Snackbar';
import Alert from '@mui/material/Alert';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Box from '@mui/material/Box';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs, { Dayjs } from 'dayjs';
import 'dayjs/locale/fr';

dayjs.locale('fr');
export type TalkStatus = 'Draft' | 'Idea' | 'Submitted' | 'Accepted' | 'Replayed';

export interface TalkData {
  id: string;
  title: string;
  speaker: string;
  cospeaker: string;
  email: string;
  agency: string;
  abstract: string;
  format: string;
  visibility: string;
  language: string;
  conference: string;
  date?: string;
  notes: string;
  status: TalkStatus;
}

export const agencyLabels: Record<string, string> = {
  paris: 'Paris',
  nantes: 'Nantes',
  rennes: 'Rennes',
  bordeaux: 'Bordeaux',
  lyon: 'Lyon',
  lille: 'Lille',
  grenoble: 'Grenoble',
  singapour: 'Singapour',
  montreal: 'Montréal',
};

export const visibilityLabels: Record<string, string> = {
  internal: 'Interne',
  external: 'Externe',
};

export const formatLabels: Record<string, string> = {
  video: 'Vidéo',
  training: 'Formation',
  public: 'Public',
  other: 'Autre',
};

export const languageLabels: Record<string, string> = {
  francais: 'Français',
  english: 'English',
};

interface CreateTalkDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (talk: TalkData) => void;
}

export function CreateTalkDialog({ open, onClose, onSubmit }: CreateTalkDialogProps) {
  const [title, setTitle] = React.useState('');
  const [speaker, setSpeaker] = React.useState('');
  const [cospeaker, setCospeaker] = React.useState('');
  const [email, setEmail] = React.useState('');
  const [abstract, setAbstract] = React.useState('');
  const [format, setFormat] = React.useState('');
  const [visibility, setVisibility] = React.useState('');
  const [language, setLanguage] = React.useState('');
  const [agency, setAgency] = React.useState('');
  const [conference, setConference] = React.useState('');
  const [date, setDate] = React.useState<Dayjs | null>(null);
  const [notes, setNotes] = React.useState('');
  const [toastOpen, setToastOpen] = React.useState(false);

  const resetForm = () => {
    setTitle('');
    setSpeaker('');
    setCospeaker('');
    setEmail('');
    setAbstract('');
    setFormat('');
    setVisibility('');
    setLanguage('');
    setAgency('');
    setConference('');
    setDate(null);
    setNotes('');
  };

  const isEmailValid = (emailStr: string) => {
    if (!emailStr.trim()) return true;
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailStr.trim());
  };

  const handleSave = (status: TalkStatus) => {
    if (status !== 'Draft') {
      const requiredFieldsMissing = !title.trim() || !speaker.trim() || !abstract.trim() || !agency || !format || !visibility;
      const emailInvalid = !isEmailValid(email);

      if (requiredFieldsMissing || emailInvalid) {
        setToastOpen(true);
        return;
      }
    } else {
      if (!title.trim()) {
        setToastOpen(true);
        return;
      }
    }

    onSubmit({
      id: crypto.randomUUID(),
      title: title.trim(),
      speaker: speaker.trim(),
      cospeaker: cospeaker.trim(),
      email: email.trim(),
      agency,
      abstract: abstract.trim(),
      format,
      visibility,
      language,
      conference: conference.trim(),
      date: date ? date.format('DD-MM-YYYY') : '',
      notes: notes.trim(),
      status,
    });
    resetForm();
    onClose();
  };

  const handleSubmit = () => handleSave('Idea');
  const handleDraft = () => handleSave('Draft');

  const handleCancel = () => {
    resetForm();
    onClose();
  };

  const handleToastClose = (_event?: React.SyntheticEvent | Event, reason?: string) => {
    if (reason === 'clickaway') return;
    setToastOpen(false);
  };

  const handleFormatChange = (event: SelectChangeEvent) => {
    setFormat(event.target.value as string);
  };

  const handleVisibilityChange = (event: SelectChangeEvent) => {
    setVisibility(event.target.value as string);
  };

  const handleLanguageChange = (event: SelectChangeEvent) => {
    setLanguage(event.target.value as string);
  };

  const handleAgencyChange = (event: SelectChangeEvent) => {
    setAgency(event.target.value as string);
  };

  return (
    <Dialog onClose={handleCancel} open={open} maxWidth="md" fullWidth>
      <DialogTitle sx={{ pb: 0 }}>Nouveau Talk</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Déclarez une nouvelle idée de talk ou soumission à une conférence.
        </Typography>

        <Grid container spacing={2}>
          <Grid size={12}>
            <TextField
              label="Titre du talk"
              id="talk-title"
              required
              fullWidth
              placeholder="Ex: Building Resilient Microservices"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </Grid>

          <Grid size={6}>
            <TextField
              label="Speaker"
              id="speaker"
              required
              fullWidth
              placeholder="Prénom Nom"
              value={speaker}
              onChange={(e) => setSpeaker(e.target.value)}
            />
          </Grid>
          <Grid size={6}>
            <TextField
              label="Co-speaker"
              id="cospeaker"
              fullWidth
              placeholder="Prénom Nom (optionnel)"
              value={cospeaker}
              onChange={(e) => setCospeaker(e.target.value)}
            />
          </Grid>

          <Grid size={6}>
            <TextField
              label="Email"
              id="email"
              fullWidth
              placeholder="speaker@zenika.com"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={email.trim() !== '' && !isEmailValid(email)}
              helperText={email.trim() !== '' && !isEmailValid(email) ? 'Format d\'email invalide' : ''}
            />
          </Grid>
          <Grid size={6}>
            <FormControl fullWidth required>
              <InputLabel id="select-agency-label">Agence</InputLabel>
              <Select
                labelId="select-agency-label"
                id="select-agency"
                value={agency}
                label="Agence"
                onChange={handleAgencyChange}
              >
                {Object.entries(agencyLabels).map(([value, label]) => (
                  <MenuItem key={value} value={value}>{label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>

          <Grid size={12}>
            <TextField
              label="Abstract / Description"
              id="abstract"
              multiline
              rows={4}
              fullWidth
              placeholder="Décrivez le contenu de votre talk..."
              required
              value={abstract}
              onChange={(e) => setAbstract(e.target.value)}
              slotProps={{ input: { sx: { '& textarea': { resize: 'vertical' } } } }}
            />
          </Grid>

          <Grid size={4}>
            <FormControl fullWidth required>
              <InputLabel id="select-format-label">Format</InputLabel>
              <Select
                labelId="select-format-label"
                id="select-format"
                value={format}
                label="Format"
                onChange={handleFormatChange}
              >
                {Object.entries(formatLabels).map(([value, label]) => (
                  <MenuItem key={value} value={value}>{label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={4}>
            <FormControl fullWidth required>
              <InputLabel id="select-visibility-label">Visibilité</InputLabel>
              <Select
                labelId="select-visibility-label"
                id="select-visibility"
                value={visibility}
                label="Visibilité"
                onChange={handleVisibilityChange}
              >
                {Object.entries(visibilityLabels).map(([value, label]) => (
                  <MenuItem key={value} value={value}>{label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={4}>
            <FormControl fullWidth>
              <InputLabel id="select-language-label">Langue</InputLabel>
              <Select
                labelId="select-language-label"
                id="select-language"
                value={language}
                label="Langue"
                onChange={handleLanguageChange}
              >
                {Object.entries(languageLabels).map(([value, label]) => (
                  <MenuItem key={value} value={value}>{label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>

          <Grid size={12}>
            <TextField
              label="Conférence cible"
              id="conference"
              fullWidth
              placeholder="Ex: Devoxx France, Sunny Tech..."
              value={conference}
              onChange={(e) => setConference(e.target.value)}
            />
          </Grid>

          <Grid size={12}>
            <DatePicker
              label="Date (optionnel)"
              value={date}
              onChange={(newValue) => setDate(newValue)}
              views={['year', 'month', 'day']}
              format="DD/MM/YYYY"
              slotProps={{ textField: { fullWidth: true } }}
            />
          </Grid>

          <Grid size={12}>
            <TextField
              label="Notes / Commentaires"
              id="notes"
              multiline
              rows={4}
              fullWidth
              placeholder="Informations complémentaires..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              slotProps={{ input: { sx: { '& textarea': { resize: 'vertical' } } } }}
            />
          </Grid>
        </Grid>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button variant="outlined" onClick={handleCancel}>
          Annuler
        </Button>
        <Box sx={{ flexGrow: 1 }} />
        <Button variant="outlined" onClick={handleDraft} sx={{ color: "#bbbbbbff"}}>
          Sauvegarder en brouillon
        </Button>
        <Button variant="contained" onClick={handleSubmit}>
          Créer le talk
        </Button>
      </DialogActions>

      <Snackbar
        open={toastOpen}
        autoHideDuration={4000}
        onClose={handleToastClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={handleToastClose} severity="error" variant="filled">
          {!isEmailValid(email) ? 'Le format de l\'email est incorrect' : !title.trim() ? 'Le titre est obligatoire, même pour un brouillon' : 'Merci de remplir tous les champs obligatoires'}
        </Alert>
      </Snackbar>
    </Dialog>
  );
}

const statusConfig: Record<TalkStatus, { text: string; bg: string }> = {
  Draft: { text: '#757575', bg: 'rgba(117, 117, 117, 0.12)' },
  Idea: { text: '#0288d1', bg: 'rgba(2, 136, 209, 0.12)' },
  Submitted: { text: '#ed6c02', bg: 'rgba(237, 108, 2, 0.12)' },
  Accepted: { text: '#2e7d32', bg: 'rgba(46, 125, 50, 0.12)' },
  Replayed: { text: '#d32f2f', bg: 'rgba(211, 47, 47, 0.12)' },
};

function StatusTag({ status }: { status: TalkStatus }) {
  const config = statusConfig[status];
  return (
    <Box
      sx={{
        px: 1.5,
        py: 0.5,
        borderRadius: 1,
        display: 'inline-block',
        fontSize: '0.75rem',
        fontWeight: 'bold',
        color: config.text,
        backgroundColor: config.bg,
      }}
    >
      {status}
    </Box>
  );
}

interface TalkDetailsDialogProps {
  talk: TalkData | null;
  open: boolean;
  onClose: () => void;
  onUpdate: (talk: TalkData) => void;
  onDelete: (id: string) => void;
}

export function TalkDetailsDialog({ talk, open, onClose, onUpdate, onDelete }: TalkDetailsDialogProps) {
  if (!talk) return null;

  const handleStatusChange = (event: SelectChangeEvent) => {
    onUpdate({ ...talk, status: event.target.value as TalkStatus });
  };

  const handleVisibilityToggle = (event: React.ChangeEvent<HTMLInputElement>) => {
    onUpdate({ ...talk, visibility: event.target.checked ? 'external' : 'internal' });
  };

  const handleDelete = () => {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce talk ?')) {
      onDelete(talk.id);
      onClose();
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ fontWeight: 'bold', pb: 1 }}>
        {talk.title}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <Grid container spacing={2}>
            <Grid size={6}>
              <Stack spacing={2}>
                <Box>
                  <Typography variant="caption" color="text.secondary">Speaker</Typography>
                  <Typography variant="body1">{talk.speaker}{talk.cospeaker ? ` & ${talk.cospeaker}` : ''}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">Conférence</Typography>
                  <Typography variant="body1">{talk.conference || '—'}</Typography>
                </Box>
              </Stack>
            </Grid>
            <Grid size={6}>
              <Stack spacing={2}>
                <Box>
                  <Typography variant="caption" color="text.secondary">Agence</Typography>
                  <Typography variant="body1">{agencyLabels[talk.agency] || talk.agency}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">Date</Typography>
                  <Typography variant="body1">{talk.date || '—'}</Typography>
                </Box>
              </Stack>
            </Grid>
          </Grid>

          <Divider />

          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <FormControl fullWidth>
              <InputLabel id="status-select-label">Statut</InputLabel>
              <Select
                labelId="status-select-label"
                value={talk.status}
                label="Statut"
                onChange={handleStatusChange}
                size="small"
              >
                {Object.keys(statusConfig).map((status) => (
                  <MenuItem key={status} value={status}>
                    {status}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

          </Box>

          <Box>
            <FormControlLabel
              control={
                <Switch
                  checked={talk.visibility === 'external'}
                  onChange={handleVisibilityToggle}
                  color="primary"
                />
              }
              label={talk.visibility === 'external' ? 'Externe' : 'Interne'}
            />
          </Box>
        </Stack>
      </DialogContent>
      <DialogActions sx={{ p: 3, justifyContent: 'space-between' }}>
        <Button color="error" variant="contained" onClick={handleDelete}>
          Supprimer
        </Button>
        <Button onClick={onClose} variant="outlined">
          Fermer
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function TalkDashboard() {
  const [open, setOpen] = React.useState(false);
  const [talks, setTalks] = React.useState<TalkData[]>([]);
  const [selectedTalkId, setSelectedTalkId] = React.useState<string | null>(null);

  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  const handleSubmit = (talk: TalkData) => {
    setTalks((prev) => [...prev, talk]);
  };

  const handleUpdateTalk = (updatedTalk: TalkData) => {
    setTalks((prev) => prev.map((t) => (t.id === updatedTalk.id ? updatedTalk : t)));
  };

  const handleDeleteTalk = (id: string) => {
    setTalks((prev) => prev.filter((t) => t.id !== id));
  };

  const selectedTalk = talks.find((t) => t.id === selectedTalkId) || null;

  return (
    <Box sx={{ p: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>

        <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
          Talks
        </Typography>
      <Typography variant="caption">
        Manage the lifecycle of talks from idea to replay.
      </Typography>
        </Box>
        <Button 
          variant="contained" 
          onClick={handleOpen} 
          sx={{ 
            fontWeight: 'bold',
            background: 'linear-gradient(135deg, #ed213c 0%, #BF1D67 100%)',
            '&:hover': {
              transform: 'translateY(-2px)',
              transition: 'transform 0.2s ease-in-out',
            }
          }}
        >
          + New Talk
        </Button>
      </Box>

      <TableContainer component={Paper} variant="outlined" sx={{ border: '1px solid #ed213c', overflow: 'hidden', borderRadius: 1 }}>
        <Table>
          <TableHead sx={{ backgroundColor: '#ececec'}}>
            <TableRow>
              <TableCell><strong>Title</strong></TableCell>
              <TableCell><strong>Speaker</strong></TableCell>
              <TableCell><strong>Office</strong></TableCell>
              <TableCell><strong>Conference</strong></TableCell>
              <TableCell><strong>Status</strong></TableCell>
              <TableCell><strong>Visibilité</strong></TableCell>
              <TableCell><strong>Actions</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody sx={{ '& tr:last-child td': { borderBottom: 0 } }}>
            {talks.length === 0 ? (
              <TableRow>
                <TableCell colSpan={9} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                  Aucun talk pour le moment. Créez-en-un avec "New Talk" !
                </TableCell>
              </TableRow>
            ) : (
              talks.map((talk) => (
                <TableRow key={talk.id} hover>
                  <TableCell>
                    <Typography
                      variant="body2"
                      sx={{
                        cursor: 'pointer',
                        color: 'primary.main',
                        textDecoration: 'underline',
                        fontWeight: 'medium',
                        '&:hover': { color: 'primary.dark' }
                      }}
                      onClick={() => setSelectedTalkId(talk.id)}
                    >
                      {talk.title}
                    </Typography>
                  </TableCell>
                  <TableCell>{talk.speaker}</TableCell>
                  <TableCell>{agencyLabels[talk.agency] || '—'}</TableCell>
                  <TableCell>{talk.conference || '—'}</TableCell>
                  <TableCell>
                    <StatusTag status={talk.status} />
                  </TableCell>
                  <TableCell>{visibilityLabels[talk.visibility] || '—'}</TableCell>
                  <TableCell>{'TODO Actions'}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <CreateTalkDialog open={open} onClose={handleClose} onSubmit={handleSubmit} />

      <TalkDetailsDialog
        talk={selectedTalk}
        open={!!selectedTalkId}
        onClose={() => setSelectedTalkId(null)}
        onUpdate={handleUpdateTalk}
        onDelete={handleDeleteTalk}
      />
    </Box>
  );
}